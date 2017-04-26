/*
 * Copyright (C) 2017 Oasis Feng. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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

/**
 * <h1>Project Condom</h1>
 *
 * <p>Project Condom is a thin library to wrap the naked Context in your Android project before passing it to the 3rd-party SDK.
 * It is designed to prevent the 3rd-party SDK from common unwanted behaviors which may harm the user experience of your app.
 *
 * <ul>
 * <li> Massive launch of processes in other apps (common in 3rd-party push SDKs), causing slow app starting and notable lagging
 * on low to middle-end devices. This behavior has "chain reaction" effects among apps with similar SDKs, greatly aggravating
 * the overall device performance.
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <ol>
 * <li>Add dependency to this library in build.gradle of your project module.
 *
 * <pre> compile 'com.oasisfeng.condom:library:1.+' </pre>
 *
 * <li>Migration the initialization code of 3rd-party SDK.
 *
 * <p>Most 3rd-party SDKs require explicit initialization with a <code>Context</code> instance, something like:
 *
 * <pre>XxxClient.init(context, ...);</pre>
 *
 * Just change the context parameter to {@link com.oasisfeng.condom.CondomContext#wrap(android.content.Context, java.lang.String)}, like this:
 *
 * <pre>XxxClient.init(CondomContext.wrap(context, "XxxSDK"), ...);</pre>
 *
 * <li>If the 3rd-party SDK contains its own components (&lt;activity&gt;, &lt;service&gt;, &lt;receiver&gt; or &lt;provider&gt;),
 * they will not be running with <code>CondomContext</code>. To also prevent them from unwanted behaviors,
 * {@link com.oasisfeng.condom.CondomProcess} is introduced to apply the process-level condom protection, assuming that
 * those components are already isolated from your application process (with separate <code>android:process</code> specified).
 *
 * <p>Add the following initialization code in the very beginning of your Application.onCreate().
 *
 * <pre>
 * public class MyApplication extends Application {
 *   public void onCreate() {
 *     CondomProcess.installExceptDefaultProcess(this);
 *     ...
 *   }
 * }</pre>
 * </ol>
 *
 * That's all! Enjoy the pleasure with the confidence of protection.
 */
@ParametersAreNonnullByDefault
package com.oasisfeng.condom;

import javax.annotation.ParametersAreNonnullByDefault;