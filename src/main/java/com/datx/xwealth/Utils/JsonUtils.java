package com.datx.xwealth.Utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class JsonUtils {

    public static String readContentFileJson(String path) {
        try {
            Resource companyDataResource = new ClassPathResource(path);
            File file = companyDataResource.getFile();
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
