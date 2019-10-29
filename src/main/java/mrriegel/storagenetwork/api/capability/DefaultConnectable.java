package mrriegel.storagenetwork.api.capability;

import mrriegel.storagenetwork.api.data.DimPos;

public class DefaultConnectable implements IConnectable {

    private DimPos master;
    private DimPos self;

    @Override
    public DimPos getMasterPos() {
        return master;
    }

    @Override
    public void setMasterPos(DimPos masterPos) {
        this.master = masterPos;
    }

    @Override
    public DimPos getPos() {
        return self;
    }

    public void setPos(DimPos pos) {
        this.self = pos;
    }
}
