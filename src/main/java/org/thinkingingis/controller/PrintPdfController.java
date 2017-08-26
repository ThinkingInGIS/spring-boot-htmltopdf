package org.thinkingingis.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.thinkingingis.model.PdfFileRequest;

@Controller
@RequestMapping("/print")
public class PrintPdfController {
	private final RestTemplate restTemplate;
	
    public PrintPdfController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }
    
    @RequestMapping(value = "/pdf", method = RequestMethod.GET)
    public void createPdfFromUrl(HttpServletResponse response) {
        PdfFileRequest fileRequest = new PdfFileRequest();
        fileRequest.setFileName("index.pdf");
        fileRequest.setSourceHtmlUrl("http://blog.csdn.net/gisboygogogo/article/");
 
        byte[] pdfFile = restTemplate.postForObject("http://localhost:8080/api/pdf", 
                fileRequest, 
                byte[].class
        );
        writePdfFileToResponse(pdfFile, "index.pdf", response);
    }
 
    private void writePdfFileToResponse(byte[] pdfFile, String fileName, HttpServletResponse response) {
        try (InputStream in = new ByteArrayInputStream(pdfFile)) {
            OutputStream out = response.getOutputStream();
            IOUtils.copy(in, out);
            out.flush();
 
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        catch (IOException ex) {
            throw new RuntimeException("Error occurred when creating PDF file", ex);
        }
    }

}
