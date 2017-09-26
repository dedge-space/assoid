/*
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.chenai.nakam.assoid.compat

import android.app.Activity
import android.content.Context
import hobby.chenai.nakam.basis.TAG._

/**
  * TypeBring for compat activity.
  *
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 11/09/2017
  */
// TypeBring 会引起其它问题，所以不要了，至少能编过。
trait AssoidCompat /* extends TypeBring[Null, AnyRef, AnyRef] */ {
  implicit lazy val context: Context = this.ensuring(isInstanceOf[Activity],
    "本特质仅限用于 Activity。".tag).asInstanceOf[Activity]
}
