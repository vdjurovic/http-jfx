module co.bitshifted.libs.httpjfx {
    requires static java.net.http;
    requires com.fasterxml.jackson.databind;

    exports co.bitshifted.reflex;
    exports co.bitshifted.reflex.http;
    exports co.bitshifted.reflex.serialize;
}