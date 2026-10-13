import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/** Build-process workaround for broken Windows AF_UNIX selector wake-up pipes. */
public final class TcpOnlyAgent {
    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            return;
        }
        Field instance = Unsafe.class.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        Unsafe unsafe = (Unsafe) instance.get(null);
        Class<?> sockets = Class.forName("sun.nio.ch.UnixDomainSockets");
        Field supported = sockets.getDeclaredField("supported");
        unsafe.putBooleanVolatile(unsafe.staticFieldBase(supported), unsafe.staticFieldOffset(supported), false);
    }
}
