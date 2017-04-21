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

package com.oasisfeng.condom;

/**
 * The callback for outbound request filtering.
 *
 * Created by Oasis on 2017/4/21.
 */
public interface OutboundJudge {
	/**
	 * Judge the outbound request or query by its explicit target package. For query requests, this will be called for each candidate,
	 * before additional filtering (e.g. {@link CondomContext#preventServiceInBackgroundPackages(boolean)}) is applied.
	 *
	 * <p>Note: Implicit broadcast will never go through this.
	 *
	 * @return whether this outbound request should be allowed, or whether the query result entry should be included in the returned collection.
	 *         Disallowed service request will simply fail and broadcast will be dropped.
	 */
	boolean shouldAllow(OutboundType type, String target_pkg);
}
