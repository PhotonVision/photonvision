package org.photonvision.common.configuration;

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import io.avaje.jsonb.Json;
import org.photonvision.common.hardware.statusLED.GreenYellowStatusLED;
import org.photonvision.common.hardware.statusLED.RGBStatusLED;
import org.photonvision.common.hardware.statusLED.SPIStatusLED;
import org.photonvision.common.hardware.statusLED.StatusLED;

@Json(typeProperty = "type")
@Json.SubTypes({
    @Json.SubType(type = RGBStatusLED.Config.class, name = "RGB"),
    @Json.SubType(type = GreenYellowStatusLED.Config.class, name = "GreenYellow"),
    @Json.SubType(type = SPIStatusLED.Config.class, name = "SPI"),
})
public interface StatusLedConfig {
    public StatusLED create(NativeDeviceFactoryInterface deviceFactory);

    public int[] pins();

    @Override
    public String toString();
}
