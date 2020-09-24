package org.photonvision.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class NetworkConfigTest {
    @Test
    public void testSerialization() throws IOException {
        var mapper = new ObjectMapper();
        var path = Path.of("netTest.json");
        mapper.writeValue(path.toFile(), new NetworkConfig());
        var in = mapper.readValue(path.toFile(), NetworkConfig.class);
        System.out.println(in);
    }
}
