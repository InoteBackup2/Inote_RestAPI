package fr.inote.inoteApi.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import fr.inote.inoteApi.crossCutting.exceptions.InoteFileNotFoundException;
import jakarta.annotation.PostConstruct;

@Service
public class FileSystemStorageService {

  private final Path rootLocation;

  @Autowired
  public FileSystemStorageService(String storageFolderPath) {
    this.rootLocation = Paths.get(storageFolderPath);
  }

  @PostConstruct
  public void init() throws IOException {
    if (!Files.exists(rootLocation)) {
      Files.createDirectories(rootLocation);
    }

  }

  // public String store(MultipartFile file) {
  // String filename = StringUtils.cleanPath(file.getOriginalFilename());
  // try {
  // if (file.isEmpty()) {
  // }
  // if (filename.contains("..")) {
  // }
  // try (InputStream inputStream = file.getInputStream()) {
  // Files.copy(inputStream, this.rootLocation.resolve(filename),
  // StandardCopyOption.REPLACE_EXISTING);
  // }
  // } catch (IOException e) {
  // }
  // return filename;
  // }

  // public Stream<Path> loadAll() {
  // try {
  // return Files.walk(this.rootLocation, 1).filter(path ->
  // !path.equals(this.rootLocation))
  // .map(this.rootLocation::relativize);
  // } catch (IOException e) {
  // return null;
  // }

  // }

  public Path load(String filename) throws InvalidPathException {
    return rootLocation.resolve(filename);
  }

  public Resource loadAsResource(String filename) throws MalformedURLException, InoteFileNotFoundException {
    
    Path file = load(filename);
    Resource resource = new UrlResource(file.toUri());
    if (!resource.exists() || !resource.isReadable()) {
      throw new InoteFileNotFoundException();
    }
    
    return resource;
  }

  // public void deleteAll() {
  // FileSystemUtils.deleteRecursively(rootLocation.toFile());
  // }

}