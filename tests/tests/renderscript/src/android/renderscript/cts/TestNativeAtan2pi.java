/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Don't edit this file!  It is auto-generated by frameworks/rs/api/gen_runtime.

package android.renderscript.cts;

import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;
import android.renderscript.Element;

public class TestNativeAtan2pi extends RSBaseCompute {

    private ScriptC_TestNativeAtan2pi script;
    private ScriptC_TestNativeAtan2piRelaxed scriptRelaxed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script = new ScriptC_TestNativeAtan2pi(mRS);
        scriptRelaxed = new ScriptC_TestNativeAtan2piRelaxed(mRS);
    }

    private void checkNativeAtan2piFloatFloatFloat() {
        Allocation inY = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 1, 0x39c4f8fd35fc5dc8l, false);
        Allocation inX = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 1, 0x39c4f8fd35fc5dc7l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 1), INPUTSIZE);
            script.set_gAllocInX(inX);
            script.forEach_testNativeAtan2piFloatFloatFloat(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloatFloatFloat: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 1), INPUTSIZE);
            scriptRelaxed.set_gAllocInX(inX);
            scriptRelaxed.forEach_testNativeAtan2piFloatFloatFloat(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloatFloatFloat: " + e.toString());
        }
    }

    private void checkNativeAtan2piFloat2Float2Float2() {
        Allocation inY = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 2, 0x6aff980c8d47a3e2l, false);
        Allocation inX = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 2, 0x6aff980c8d47a3e1l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 2), INPUTSIZE);
            script.set_gAllocInX(inX);
            script.forEach_testNativeAtan2piFloat2Float2Float2(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloat2Float2Float2: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 2), INPUTSIZE);
            scriptRelaxed.set_gAllocInX(inX);
            scriptRelaxed.forEach_testNativeAtan2piFloat2Float2Float2(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloat2Float2Float2: " + e.toString());
        }
    }

    private void checkNativeAtan2piFloat3Float3Float3() {
        Allocation inY = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 3, 0xbf64762c8f25a583l, false);
        Allocation inX = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 3, 0xbf64762c8f25a582l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 3), INPUTSIZE);
            script.set_gAllocInX(inX);
            script.forEach_testNativeAtan2piFloat3Float3Float3(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloat3Float3Float3: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 3), INPUTSIZE);
            scriptRelaxed.set_gAllocInX(inX);
            scriptRelaxed.forEach_testNativeAtan2piFloat3Float3Float3(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloat3Float3Float3: " + e.toString());
        }
    }

    private void checkNativeAtan2piFloat4Float4Float4() {
        Allocation inY = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 4, 0x13c9544c9103a724l, false);
        Allocation inX = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 4, 0x13c9544c9103a723l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 4), INPUTSIZE);
            script.set_gAllocInX(inX);
            script.forEach_testNativeAtan2piFloat4Float4Float4(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloat4Float4Float4: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 4), INPUTSIZE);
            scriptRelaxed.set_gAllocInX(inX);
            scriptRelaxed.forEach_testNativeAtan2piFloat4Float4Float4(inY, out);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testNativeAtan2piFloat4Float4Float4: " + e.toString());
        }
    }

    public void testNativeAtan2pi() {
        checkNativeAtan2piFloatFloatFloat();
        checkNativeAtan2piFloat2Float2Float2();
        checkNativeAtan2piFloat3Float3Float3();
        checkNativeAtan2piFloat4Float4Float4();
    }
}