/*
 *
 *  * Copyright (c) 2023-2023  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  
 */

package co.bitshifted.reflex.http;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public record RFXHttpRequest<T>(RFXHttpMethod method, URI uri,
                                Optional<T> body, Optional<RFXHttpHeaders> headers) {
    public RFXHttpRequest{
        Objects.requireNonNull(method);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(body);
        Objects.requireNonNull(headers);
    }
}
