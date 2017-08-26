package org.thinkingingis.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thinkingingis.model.PdfFileRequest;

@Service
public class PdfFileCreator {
	 private static final Logger LOGGER = LoggerFactory.getLogger(PdfFileCreator.class);
	 public void writePdfToResponse(PdfFileRequest fileRequest, HttpServletResponse response) {
	        String pdfFileName = fileRequest.getFileName();
	        requireNotNull(pdfFileName, "The file name of the created PDF must be set");
	        requireNotEmpty(pdfFileName, "File name of the created PDF cannot be empty");
	 
	        String sourceHtmlUrl = fileRequest.getSourceHtmlUrl();
	        requireNotNull(sourceHtmlUrl, "Source HTML url must be set");
	        requireNotEmpty(sourceHtmlUrl, "Source HTML url cannot be empty");
	 
	        List<String> pdfCommand = Arrays.asList(
	                "wkhtmltopdf",
	                sourceHtmlUrl,
	                "-"
	        );
	 
	        ProcessBuilder pb = new ProcessBuilder(pdfCommand);
	        Process pdfProcess;
	 
	        try {
	            pdfProcess = pb.start();
	 
	            try(InputStream in = pdfProcess.getInputStream()) {
	                writeCreatedPdfFileToResponse(in, response);
	                waitForProcessBeforeContinueCurrentThread(pdfProcess);
	                requireSuccessfulExitStatus(pdfProcess);
	                setResponseHeaders(response, fileRequest);
	            }
	            catch (Exception ex) {
	                writeErrorMessageToLog(ex, pdfProcess);
	                throw new RuntimeException("PDF generation failed");
	            }
	            finally {
	                pdfProcess.destroy();
	            }
	        }
	        catch (IOException ex) {
	            throw new RuntimeException("PDF generation failed");
	        }
	    }
	 
	    private void requireNotNull(String value, String message) {
	        if (value == null) {
	            throw new IllegalArgumentException(message);
	        }
	    }
	 
	    private void requireNotEmpty(String value, String message) {
	        if (value.isEmpty()) {
	            throw new IllegalArgumentException(message);
	        }
	    }
	 
	    private void writeCreatedPdfFileToResponse(InputStream in, HttpServletResponse response) throws IOException {
	        OutputStream out = response.getOutputStream();
	        IOUtils.copy(in, out);
	        out.flush();
	    }
	 
	    private void waitForProcessBeforeContinueCurrentThread(Process process) {
	        try {
	            process.waitFor(2, TimeUnit.SECONDS);
	        }
	        catch (InterruptedException ex) {
	            Thread.currentThread().interrupt();
	        }
	    }
	 
	    private void requireSuccessfulExitStatus(Process process) {
	        if (process.exitValue() != 0) {
	            throw new RuntimeException("PDF generation failed");
	        }
	    }
	 
	    private void setResponseHeaders(HttpServletResponse response, PdfFileRequest fileRequest) {
	        response.setContentType("application/pdf");
	        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileRequest.getFileName() + "\"");
	    }
	 
	    private void writeErrorMessageToLog(Exception ex, Process pdfProcess) throws IOException {
	        LOGGER.error("Could not create PDF because an exception was thrown: ", ex);
	        LOGGER.error("The exit value of PDF process is: {}", pdfProcess.exitValue());
	 
	        String errorMessage = getErrorMessageFromProcess(pdfProcess);
	        LOGGER.error("PDF process ended with error message: {}", errorMessage);
	    }
	 
	    private String getErrorMessageFromProcess(Process pdfProcess) {
	        try {
	            BufferedReader reader = new BufferedReader(new InputStreamReader(pdfProcess.getErrorStream()));
	            StringWriter writer = new StringWriter();
	 
	            String line;
	            while ((line = reader.readLine()) != null) {
	                writer.append(line);
	            }
	 
	            return writer.toString();
	        }
	        catch (IOException ex) {
	            LOGGER.error("Could not extract error message from process because an exception was thrown", ex);
	            return "";
	        }
	    }

}
