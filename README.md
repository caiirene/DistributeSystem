Here's a README for your Project 3 based on the format provided:

# CS6650 [Project3] -- IRENE CAI -- Java RMI Server and Client with 2PC

This project extends the previous key-value store implementation using Java RMI (Remote Method Invocation) to include a two-phase commit (2PC) protocol for ensuring consistency across multiple server replicas.

## Files

- `RMIInterface.java`: Defines the remote interface for the key-value store.
- `RMIImplement.java`: Implements the remote interface and provides the logic for the key-value store and the 2PC protocol.
- `RMIServer.java`: The main entry point for the server application. It sets up the RMI registry, binds the remote object, and manages multiple server replicas.
- `RMIClient.java`: The main entry point for the client application. It looks up the remote objects in the RMI registry and interacts with them, utilizing the 2PC protocol for PUT and DELETE operations.

## Usage

1. To compile the files, use the following command:
   ```bash
   javac *.java
   ```
2. Start the RMI servers with NO numbers:
   ```bash
   java RMIServer
   ```
   *Note: The server will start 5 replicas on consecutive ports starting from 2000.
3. Run the RMI client:
   ```bash
   java RMIClient
   ```
   
## Example
Start the RMI servers with the command:

```bash
java RMIServer
```
Start the RMI client to connect to the servers:

```bash
java RMIClient
```
The client will prompt for commands to interact with the server's key-value store, and PUT and DELETE operations will be performed using the 2PC protocol to ensure consistency across all server replicas.
```bash
Please enter your command (enter stop to stop): <put/get/delete> <key> <value(put only)>
```
End the RMI client to connect to the servers:
```bash
Please enter your command (enter stop to stop): stop
```