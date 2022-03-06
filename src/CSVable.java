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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (!declaredMethod.trySetAccessible()) continue;
            CSVCalculate csv = declaredMethod.getAnnotation(CSVCalculate.class);
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
            CSVInclude annotation = declaredField.getAnnotation(CSVInclude.class);
            boolean shouldPrint = annotation != null;

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
                    sb.append(annotation.valueIfNull());
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
        Method[] declaredMethods = this.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (!declaredMethod.trySetAccessible()) continue;
            CSVCalculate annotation = declaredMethod.getAnnotation(CSVCalculate.class);
            boolean shouldPrint = annotation != null;
            if (shouldPrint) {
                boolean erred = false;
                if (shouldPrintComma) {
                    sb.append(",");
                }
                int paramCount = annotation.parameterNames() == null ? 0 : annotation.parameterNames().length;
                if (declaredMethod.getParameterCount() != paramCount) {
                    sb.append("Error: Invalid parameter length");
                    continue;
                }
                Object[] parameters = new Object[paramCount];
                for (int i = 0; i < paramCount; i++) {
                    try {
                        Field field = this.getClass().getDeclaredField(annotation.parameterNames()[i]);
                        field.trySetAccessible();
                        boolean aStatic = Modifier.isStatic(field.getModifiers());
                        Object o = field.get(this);
                        parameters[i] = o;
                    } catch (NoSuchFieldException e) {
                        sb.append("Error: No field declared by \"").append(annotation.parameterNames()[i]).append("\"");
                        erred = true;
                    } catch (IllegalAccessException e) {
                        sb.append("Error: Failed to access field");
                        erred = true;
                    }
                }
                if (erred) continue;
                Object o = null;
                try {
                    o = declaredMethod.invoke(this, parameters);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    sb.append("Error: ").append(e);
                    e.printStackTrace();
                    continue;
                }
                if (o == null) {
                    sb.append(annotation.valueIfNull());
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
}

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface CSVInclude {
    String value();

    String valueIfNull() default "";
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface CSVCalculate {
    String value();

    String[] parameterNames() default {};

    String valueIfNull() default "";
}