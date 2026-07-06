package chat.delta.rpc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** JSON-RPC transport over a deltachat-rpc-server subprocess (stdin/stdout). */
public class StdioRpcTransport extends BaseRpcTransport {
  private final Process process;
  private final BufferedWriter writer;
  private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

  public StdioRpcTransport(Process process) {
    this.process = process;
    this.writer =
        new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    Thread readerThread =
        new Thread(
            () -> {
              try {
                String line;
                while ((line = reader.readLine()) != null) {
                  responseQueue.put(line);
                }
              } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            },
            "rpc-stdio-reader");
    readerThread.setDaemon(true);
    readerThread.start();
  }

  @Override
  protected void sendRequest(String jsonRequest) {
    try {
      writer.write(jsonRequest);
      writer.newLine();
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("RPC send failed", e);
    }
  }

  @Override
  protected String getResponse() {
    try {
      return responseQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("RPC read interrupted", e);
    }
  }

  public Process getProcess() {
    return process;
  }
}
