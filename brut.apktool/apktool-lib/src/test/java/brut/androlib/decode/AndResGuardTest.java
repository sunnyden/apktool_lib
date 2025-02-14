/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.decode;

import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import java.io.File;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;

public class AndResGuardTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(AndResGuardTest.class, "decode/issue1170/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void checkifAndResDecodeRemapsRFolder() throws BrutException, IOException {
        String apk = "issue1170.apk";

        // decode issue1170.apk
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk), InstrumentationRegistry.getInstrumentation().getContext());
        sTestOrigDir = new ExtFile(sTmpDir + File.separator + apk + ".out");

        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".out"));
        apkDecoder.decode();

        File aPng =  new File(sTestOrigDir,"res/mipmap-hdpi-v4/a.png");
        assertTrue(aPng.isFile());
    }

    @Test
    public void checkifAndResDecodeRemapsRFolderInRawMode() throws BrutException, IOException {
        String apk = "issue1170.apk";
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk),InstrumentationRegistry.getInstrumentation().getContext());
        sTestOrigDir = new ExtFile(sTmpDir + File.separator + apk + ".raw.out");

        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".raw.out"));
        apkDecoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_NONE);
        apkDecoder.decode();

        File aPng =  new File(sTestOrigDir,"r/a/a.png");
        assertTrue(aPng.isFile());
    }
}
