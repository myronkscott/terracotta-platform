/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.persistence.sanskrit.change.SanskritChange;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The main class for reading and writing a Sanskrit append log.
 * This class is intended to be used by a single thread and so it is not thread-safe.
 */
public class SanskritImpl implements Sanskrit {
  private static final String APPEND_LOG_FILE = "append.log";
  private static final String HASH_0_FILE = "hash0";
  private static final String HASH_1_FILE = "hash1";

  private final FilesystemDirectory filesystemDirectory;
  private final ObjectMapper objectMapper;
  private MutableSanskritObject data;
  private String lastHash;
  private String nextHashFile;

  public SanskritImpl(FilesystemDirectory filesystemDirectory, ObjectMapper objectMapper) throws SanskritException {
    this.filesystemDirectory = filesystemDirectory;
    this.objectMapper = objectMapper;
    this.data = newMutableSanskritObject();

    try {
      List<String> filesToDelete = new ArrayList<>();
      String hash0 = getHashFromFile(HASH_0_FILE, filesToDelete);
      String hash1 = getHashFromFile(HASH_1_FILE, filesToDelete);
      HashChecker hashChecker = new HashChecker(hash0, hash1);

      MutableSanskritObject result = newMutableSanskritObject();

      try (FileData appendLog = filesystemDirectory.getFileData(APPEND_LOG_FILE)) {
        if (appendLog != null) {
          InputStream appendLogStream = new BufferedInputStream(Channels.newInputStream(appendLog));
          MarkableLineParser parser = new MarkableLineParser(appendLogStream);
          Stream<String> lines = parser.lines();
          Stream<Deque<String>> records = groupByEmptyLines(lines);

          AtomicReference<SanskritException> error = new AtomicReference<>();
          try {
            records.forEach(record -> {
              try {
                if (record.size() < 3) {
                  throw new SanskritException("Invalid record");
                }

                String timestamp = record.removeFirst();
                String hash = record.removeLast();
                String json = record.stream().collect(Collectors.joining(System.lineSeparator()));

                checkHash(timestamp, json, hash);
                String hashedHash = HashUtils.generateHash(hash);
                boolean acceptRecord = hashChecker.check(hashedHash);

                if (acceptRecord) {
                  parser.mark();
                  JsonUtils.parse(objectMapper, json, result);
                  lastHash = hash;
                }
              } catch (SanskritException e) {
                error.set(e);
                throw new UncheckedSanskritException(e);
              }
            });
          } catch (UncheckedSanskritException e) {
            if (error.get() != null) {
              throw error.get();
            } else {
              throw e;
            }
          }

          long mark = parser.getMark();
          if (mark == 0) {
            filesToDelete.add("append.log");
          } else {
            try {
              appendLog.truncate(mark);
            } catch (IOException e) {
              throw new SanskritException(e);
            }
          }
        }
      }

      String hashToDelete = hashChecker.done();
      if (hashToDelete != null) {
        filesToDelete.add(hashToDelete);
      }

      for (String file : filesToDelete) {
        filesystemDirectory.delete(file);
      }

      nextHashFile = hashChecker.nextHashFile();

      this.data = result;
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private String getHashFromFile(String hashFile, List<String> filesToDelete) throws SanskritException {
    ByteBuffer hashBuffer = ByteBuffer.allocate(40);

    try (FileData fileData = filesystemDirectory.getFileData(hashFile)) {
      if (fileData == null) {
        return null;
      }

      if (fileData.size() > 40) {
        throw new SanskritException("Hash file too long: " + hashFile);
      }

      if (fileData.size() < 40) {
        filesToDelete.add(hashFile);
        return null;
      }

      while (hashBuffer.hasRemaining()) {
        int read = fileData.read(hashBuffer);
        if (read == -1) {
          break;
        }
      }
    } catch (IOException e) {
      throw new SanskritException(e);
    }

    hashBuffer.flip();
    return StandardCharsets.UTF_8.decode(hashBuffer).toString();
  }

  private void checkHash(String timestamp, String json, String hash) throws SanskritException {
    String expectedHash = calculateHash(timestamp, json);
    if (!hash.equals(expectedHash)) {
      // Don't add expectedHash to the error - the customer will just go and change the file!
      throw new SanskritException("Hash mismatch: " + hash);
    }
  }

  private String calculateHash(String timestamp, String json) {
    if (lastHash == null) {
      return HashUtils.generateHash(
          timestamp,
          System.lineSeparator(),
          json
      );
    } else {
      return HashUtils.generateHash(
          lastHash,
          System.lineSeparator(),
          System.lineSeparator(),
          timestamp,
          System.lineSeparator(),
          json
      );
    }
  }

  private Stream<Deque<String>> groupByEmptyLines(Stream<String> lines) {
    return StreamSupport.stream(new GroupingSpliterator(lines), false);
  }

  @Override
  public void close() {
  }

  @Override
  public String getString(String key) {
    return data.getString(key);
  }

  @Override
  public Long getLong(String key) {
    return data.getLong(key);
  }

  @Override
  public SanskritObject getObject(String key) {
    return CopyUtils.makeCopy(objectMapper, data.getObject(key));
  }

  @Override
  public void applyChange(SanskritChange change) throws SanskritException {
    change.accept(data);
    append(change);
  }

  @Override
  public MutableSanskritObject newMutableSanskritObject() {
    return new SanskritObjectImpl(objectMapper);
  }

  private void append(SanskritChange change) throws SanskritException {
    String json = changeAsJson(change);
    append(json);
  }

  private String changeAsJson(SanskritChange change) throws SanskritException {
    JsonSanskritChangeVisitor visitor = new JsonSanskritChangeVisitor(objectMapper);
    change.accept(visitor);
    return visitor.getJson();
  }

  private void append(String json) throws SanskritException {
    StringBuilder logEntry = new StringBuilder();

    String timestamp = getTimestamp();
    String hash = calculateHash(timestamp, json);

    logEntry.append(timestamp);
    logEntry.append(System.lineSeparator());
    logEntry.append(json);
    logEntry.append(System.lineSeparator());
    logEntry.append(hash);
    logEntry.append(System.lineSeparator());
    logEntry.append(System.lineSeparator());

    append(logEntry.toString(), hash);
  }

  private String getTimestamp() {
    return Instant.now().toString();
  }

  private void append(String logEntry, String entryHash) throws SanskritException {
    String finalHash = HashUtils.generateHash(entryHash);

    try (
        FileData appendLog = getAppendLogForAppend();
        FileData hashFile = createNewHashFile()
    ) {
      write(appendLog, logEntry);
      write(hashFile, finalHash);

      nextHashFile = flipHashFile();
      filesystemDirectory.delete(nextHashFile);

      lastHash = entryHash;
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private String flipHashFile() {
    if (Objects.equals(nextHashFile, HASH_0_FILE)) {
      return HASH_1_FILE;
    }

    return HASH_0_FILE;
  }

  private FileData getAppendLogForAppend() throws SanskritException {
    try (Owner<FileData, IOException> appendLogOwner = Owner.own(filesystemDirectory.create(APPEND_LOG_FILE, true), IOException.class)) {
      FileData appendLog = appendLogOwner.borrow();
      appendLog.position(appendLog.size());
      return appendLogOwner.release();
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private FileData createNewHashFile() throws SanskritException {
    try {
      return filesystemDirectory.create(nextHashFile, false);
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private void write(FileData fileData, String text) throws SanskritException {
    try {
      ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);

      while (bytes.hasRemaining()) {
        fileData.write(bytes);
      }

      fileData.force(false);
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }
}
