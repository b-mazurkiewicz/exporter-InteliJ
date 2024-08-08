package com.example.exporter.service;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.exporter.model.FileDB;
import com.example.exporter.repository.FileDBRepository;
@Service
public class FileStorageService {

    @Autowired
    private FileDBRepository fileDBRepository;

    public FileDB store(MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        FileDB FileDB = new FileDB(fileName, file.getContentType(), file.getBytes());

        return fileDBRepository.save(FileDB);
    }

    public FileDB getFile(String id) {
        return fileDBRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("File not found with id " + id));
    }


    public Stream<FileDB> getAllFiles() {
        return fileDBRepository.findAll().stream();
    }

    public boolean deleteFile(String fileId) {
        try {
            fileDBRepository.deleteById(fileId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
