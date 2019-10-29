package mrriegel.storagenetwork.data;

public enum EnumSortType {
    AMOUNT, NAME, MOD;

    private static final EnumSortType[] vals = values();

    public EnumSortType next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}