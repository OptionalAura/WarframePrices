/*
 * Copyright 2022 Daniel Allen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public interface CSVable {
    static String getCsvHeader(Class<? extends CSVable> clazz) {
        StringBuilder sb = new StringBuilder();
        Field[] declaredFields = clazz.getDeclaredFields();
        boolean shouldPrintComma = false;
        for (Field declaredField : declaredFields) {

            if (!declaredField.trySetAccessible()) continue;
            CSVInclude csv = declaredField.getAnnotation(CSVInclude.class);
            if (csv != null) {
                if (shouldPrintComma) {
                    sb.append(",");
                }
                sb.append(csv.value());
                shouldPrintComma = true;
            }
        }
        return sb.toString();
    }

    default String toCsv() {
        StringBuilder sb = new StringBuilder();
        Field[] declaredFields = this.getClass().getDeclaredFields();
        boolean shouldPrintComma = false;
        for (Field declaredField : declaredFields) {

            if (!declaredField.trySetAccessible()) continue;
            boolean shouldPrint = declaredField.getAnnotation(CSVInclude.class) != null;

            if (shouldPrint) {
                if (shouldPrintComma) {
                    sb.append(",");
                }
                Object o = null;
                try {
                    o = declaredField.get(this);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (o == null) {
                    sb.append("null");
                } else if (o instanceof CSVable) {
                    sb.append(((CSVable) o).toCsv());
                } else if (o.getClass().isArray()) {
                    Object[] array = ((Object[]) o);
                    if (array.length == 0) {
                        sb.append("[]");
                    } else {
                        StringBuilder arraySb = new StringBuilder();
                        arraySb.append('[');
                        arraySb.append(array[0].toString());
                        for (int j = 1; j < array.length; j++) {
                            arraySb.append(";").append(array[j].toString());
                        }
                        arraySb.append(']');
                        sb.append(arraySb);
                    }
                } else {
                    sb.append(o);
                }
                shouldPrintComma = true;
            }
        }
        return sb.toString();
    }

    String fromCSV();

}

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface CSVInclude {
    String value();
}