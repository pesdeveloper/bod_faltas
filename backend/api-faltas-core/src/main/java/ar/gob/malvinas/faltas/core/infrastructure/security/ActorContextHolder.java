package ar.gob.malvinas.faltas.core.infrastructure.security;

public final class ActorContextHolder {
    private static final ThreadLocal<ActorContext> HOLDER = new ThreadLocal<>();

    private ActorContextHolder() {}

    public static void set(ActorContext ctx) { HOLDER.set(ctx); }
    public static ActorContext get() { return HOLDER.get(); }
    public static String subOr(String fallback) {
        ActorContext ctx = HOLDER.get();
        return ctx != null ? ctx.sub() : fallback;
    }
    public static void clear() { HOLDER.remove(); }
}
