package ru.danilavak.zizu.binaryapi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class MultipartMixedResponseFactory {
    public MultiValueMap<String, HttpEntity<ByteArrayResource>> create(Map<String, byte[]> parts) {
        LinkedMultiValueMap<String, HttpEntity<ByteArrayResource>> body = new LinkedMultiValueMap<>();
        for (Map.Entry<String, byte[]> entry : new LinkedHashMap<>(parts).entrySet()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(entry.getKey()).build());
            headers.setContentLength(entry.getValue().length);
            body.add("files", new HttpEntity<>(new NamedByteArrayResource(entry.getValue(), entry.getKey()), headers));
        }
        return body;
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
