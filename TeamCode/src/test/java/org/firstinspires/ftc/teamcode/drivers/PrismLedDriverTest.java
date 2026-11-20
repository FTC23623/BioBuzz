package org.firstinspires.ftc.teamcode.drivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

/** The Prism driver with a pretend I2C port: exactly which bytes go out. */
@DisplayName("PrismLedDriver")
class PrismLedDriverTest {

    @Test
    @DisplayName("is built on I2C address 0x38")
    void address() {
        I2cDeviceSynchSimple port = mock(I2cDeviceSynchSimple.class);
        new PrismLedDriver(port, true);
        ArgumentCaptor<I2cAddr> address = ArgumentCaptor.forClass(I2cAddr.class);
        verify(port).setI2cAddress(address.capture());
        assertEquals(0x38, address.getValue().get7Bit());
    }

    @Test
    @DisplayName("a layer write is the layer's register, the sub-register, then the data")
    void layerWrite() {
        I2cDeviceSynchSimple port = mock(I2cDeviceSynchSimple.class);
        PrismLedDriver prism = new PrismLedDriver(port, true);
        prism.WriteLayer(0, 0x04, new byte[] {(byte) 255, 0, 0});
        verify(port).write(eq(0x08), aryEq(new byte[] {0x04, (byte) 255, 0, 0}));
        prism.WriteLayer(3, 0x00, new byte[] {1});
        verify(port).write(eq(0x0B), aryEq(new byte[] {0x00, 1}));
    }

    @Test
    @DisplayName("SolidColor: set up dark (brightness 0, animation 1, start, stop, color), brightness last, so no flash")
    void solidColor() {
        I2cDeviceSynchSimple port = mock(I2cDeviceSynchSimple.class);
        PrismLedDriver prism = new PrismLedDriver(port, true);
        prism.SolidColor(2, 2, 3, 50, 255, 96, 0);
        InOrder order = Mockito.inOrder(port);
        order.verify(port).write(eq(0x0A), aryEq(new byte[] {0x01, 0}));
        order.verify(port).write(eq(0x0A), aryEq(new byte[] {0x00, 1}));
        order.verify(port).write(eq(0x0A), aryEq(new byte[] {0x02, 2}));
        order.verify(port).write(eq(0x0A), aryEq(new byte[] {0x03, 3}));
        order.verify(port).write(eq(0x0A), aryEq(new byte[] {0x04, (byte) 255, 96, 0}));
        order.verify(port).write(eq(0x0A), aryEq(new byte[] {0x01, 50}));
    }

    @Test
    @DisplayName("MoveRun: only start and stop, in the order asked, so a lit layer slides without changing color")
    void moveRun() {
        I2cDeviceSynchSimple port = mock(I2cDeviceSynchSimple.class);
        PrismLedDriver prism = new PrismLedDriver(port, true);
        prism.MoveRun(3, 4, 8, true);
        InOrder order = Mockito.inOrder(port);
        order.verify(port).write(eq(0x0B), aryEq(new byte[] {0x02, 4}));
        order.verify(port).write(eq(0x0B), aryEq(new byte[] {0x03, 8}));
        prism.MoveRun(3, 1, 2, false);
        order.verify(port).write(eq(0x0B), aryEq(new byte[] {0x03, 2}));
        order.verify(port).write(eq(0x0B), aryEq(new byte[] {0x02, 1}));
        verify(port, Mockito.times(4))
                .write(eq(0x0B), ArgumentMatchers.any(byte[].class));
    }

    @Test
    @DisplayName("ClearLayer: animation 0 on that layer")
    void clearLayer() {
        I2cDeviceSynchSimple port = mock(I2cDeviceSynchSimple.class);
        new PrismLedDriver(port, true).ClearLayer(9);
        verify(port).write(eq(0x11), aryEq(new byte[] {0x00, 0}));
    }

    @Test
    @DisplayName("reads the device id as 0 to 255 and the status as four bytes")
    void reads() {
        I2cDeviceSynchSimple port = mock(I2cDeviceSynchSimple.class);
        when(port.read8(PrismLedDriver.REG_DEVICE_ID)).thenReturn((byte) 0xA5);
        when(port.read(PrismLedDriver.REG_STATUS, 4)).thenReturn(new byte[] {12, 60, 0, 0});
        PrismLedDriver prism = new PrismLedDriver(port, true);
        assertEquals(0xA5, prism.ReadDeviceId());
        assertEquals(4, prism.ReadStatus().length);
    }
}
