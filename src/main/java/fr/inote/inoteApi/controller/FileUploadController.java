package fr.inote.inoteApi.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.inote.inoteApi.crossCutting.exceptions.InoteFileNotFoundException;
import fr.inote.inoteApi.service.impl.FileSystemStorageService;

@RestController
public class FileUploadController {

  private final FileSystemStorageService fileSystemStorageService;

  
  public FileUploadController(FileSystemStorageService fileSystemStorageService) {
    this.fileSystemStorageService = fileSystemStorageService;
  }

  @GetMapping("/api/files/{type}/{filename}")
  @ResponseBody
  public ResponseEntity<Resource> serveFile(@PathVariable String type, @PathVariable String filename) throws InoteFileNotFoundException, IOException {

    Resource file = this.fileSystemStorageService.loadAsResource(String.format("%s/%s", type, filename));
    String mimeType = URLConnection.guessContentTypeFromName(file.getFilename());
    long contentLength = file.contentLength();

    InputStream fileInputStream = file.getInputStream();

    return ResponseEntity.ok().contentLength(contentLength)
        .contentType(MediaType.parseMediaType(mimeType))
        .body(new InputStreamResource(fileInputStream));
  }

  // // Upload d'un fichier
  // @PostMapping("/upload")
  // public String handleFileUpload(@RequestParam("file") MultipartFile file,
  // RedirectAttributes redirectAttributes) {
  // storageService.store(file);
  // return "redirect:/";
  // }
}
