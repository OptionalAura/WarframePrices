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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

public class CSVWriter {
    private final FileOutputStream fw;
    private final FileLock fl;
    private File f;

    public CSVWriter(File f) throws IOException {
        if (f.exists()) this.f = f;
        fw = new FileOutputStream(f);
        fl = fw.getChannel().lock();
    }

    public void writeLine(String line) throws IOException {
        fw.write(line.getBytes());
        fw.write("\n".getBytes());
    }

    public void writeObject(CSVable obj) throws IOException {
        fw.write(obj.toCSV().getBytes());
        fw.write("\n".getBytes());
    }

    public void flush() throws IOException {
        fw.flush();
    }

    public void close() throws IOException {
        fl.release();
        fw.close();
    }
}