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
package brut.androlib.aapt1;

import brut.androlib.Androlib;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.meta.MetaInfo;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

import androidx.test.platform.app.InstrumentationRegistry;

public class ReferenceVersionCodeTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(ReferenceVersionCodeTest.class, "aapt1/issue1234/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void referenceBecomesLiteralTest() throws BrutException, IOException {
        String apk = "issue1234.apk";

        // decode issue1234.apk
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk), InstrumentationRegistry.getInstrumentation().getContext());
        ExtFile decodedApk = new ExtFile(sTmpDir + File.separator + apk + ".out");
        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".out"));
        apkDecoder.decode();

        MetaInfo metaInfo = new Androlib(InstrumentationRegistry.getInstrumentation().getContext()).readMetaFile(decodedApk);
        assertEquals("v1.0.0", metaInfo.versionInfo.versionName);
    }
}
