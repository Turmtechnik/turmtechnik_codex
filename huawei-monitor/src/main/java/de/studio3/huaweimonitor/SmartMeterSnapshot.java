package de.studio3.huaweimonitor;

public class SmartMeterSnapshot {
    public int activePowerWatts;
    public int reactivePowerVar;
    public int apparentPowerVa;
    public int powerFactorMilli;
    public int voltageL1DeciVolts;
    public int voltageL2DeciVolts;
    public int voltageL3DeciVolts;
    public int currentL1MilliAmps;
    public int currentL2MilliAmps;
    public int currentL3MilliAmps;
    public long importEnergyHundredthsKwh;
    public long exportEnergyHundredthsKwh;

    public static SmartMeterSnapshot fromRegisterBlock(int startAddress, int[] registers) {
        SmartMeterSnapshot snapshot = new SmartMeterSnapshot();
        snapshot.voltageL1DeciVolts = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32260));
        snapshot.voltageL2DeciVolts = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32262));
        snapshot.voltageL3DeciVolts = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32264));
        snapshot.currentL1MilliAmps = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32272));
        snapshot.currentL2MilliAmps = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32274));
        snapshot.currentL3MilliAmps = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32276));
        snapshot.activePowerWatts = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32278));
        snapshot.reactivePowerVar = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32280));
        snapshot.powerFactorMilli = ModbusTcpClient.toSignedInt16(registers, offsetOf(startAddress, 32284));
        snapshot.apparentPowerVa = ModbusTcpClient.toSignedInt32(registers, offsetOf(startAddress, 32286));
        snapshot.importEnergyHundredthsKwh = ModbusTcpClient.toSignedInt64(registers, offsetOf(startAddress, 32357));
        snapshot.exportEnergyHundredthsKwh = ModbusTcpClient.toSignedInt64(registers, offsetOf(startAddress, 32361));
        return snapshot;
    }

    private static int offsetOf(int startAddress, int registerAddress) {
        return registerAddress - startAddress;
    }
}
