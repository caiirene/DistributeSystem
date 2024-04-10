import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * RMIClient is a client for the RMI dictionary service.
 */
public class RMIClient {

    private static ArrayList<RMIInterface> stubsList = new ArrayList<>();

    /**
     * The main method that starts the client.
     *
     * @param args Command line arguments, expects a single argument: the port number.
     */
    public static void main(String[] args) {

        int port = 2000;

        for (int i = 0; i < 5; i++) {
            RMIInterface stub = null;
            try {
                // Get the registry
                Registry registry = LocateRegistry.getRegistry("localhost", port);

                // Lookup the remote object
                stub = (RMIInterface) registry.lookup("RMIDictionary" + i);

                stubsList.add(stub);
                port++;

            } catch (ConnectException e) {
                System.out.println("RMIClient error: Connection exception -- Could not connect to the registry on port " +
                        port + ". Please the port number you enter here must be same as the port you used to start server.");
                return;
            } catch (Exception e) {
                System.err.println("RMIClient exception: " + e.toString());
                return;
            }
        }
        port = 2323;
        System.out.println(stubsList);

        RMIClient client = new RMIClient();

        for (RMIInterface stub : stubsList) {
            try {
                System.out.println("Testing stub on port: " + (port + stubsList.indexOf(stub)));
                client.tryFive(stub);
            } catch (RemoteException e) {
                System.out.println("RMIClient error: tryFive unsuccessful for stub on port " + (port + stubsList.indexOf(stub)));
            }
        }

        boolean stop = false;
        Scanner scanner = new Scanner(System.in);
        int requestNum = 0;
        while (!stop) {
            requestNum = requestNum%5;
            System.out.print("Please enter your command (enter stop to stop): ");
            String input = scanner.nextLine().trim();
            if (input.equals("stop")) {
                stop = true;
                break;
            }
            String[] result = processCommand(input);
            if (result == null) {
                continue;
            } else {
                try {
                    int currentAskingPort = requestNum+port;
                    System.out.println("RMIClient: Processed command -- " + String.join(" ", result));
                    String response = callRemoteMethod(stubsList.get(requestNum), result);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    System.out.println("RMIClient send request current time to port "+currentAskingPort+": " + sdf.format(new Date(System.currentTimeMillis())));
                    System.out.println(response);
                    requestNum++;
                }catch (Exception ex) {
                    System.err.println("RMIClient error: An error occurred while callRemoteMethod: " + ex.getMessage());
                }

            }
        }
        scanner.close();
    }

    /**
     * Tries five PUT, GET, and DELETE operations on the remote dictionary.
     *
     * @param stub The remote object stub for the dictionary service.
     * @throws RemoteException If a remote method call fails.
     */
    public void tryFive(RMIInterface stub) throws RemoteException {
        try {
            System.out.println("RMIClient: First five try start.");
            for (int i = 1; i <= 5; i++) {
                System.out.println(stub.put("" + i*1000, "" + i*-20));
                System.out.println(stub.get("" + i*1000));
                System.out.println(stub.delete("" + i*1000));
            }
            System.out.println("RMIClient: First five try end.");
        } catch (RemoteException e) {
            System.out.println("RMIClient error: tryFive unsuccessful.");
        }
    }
    /**
     * Processes a command string into an array of its components.
     *
     * @param command The command string to process.
     * @return An array of command components, or null if the command is invalid.
     */
    public static String[] processCommand(String command) {
        String[] parts = command.split("\\s+"); // 使用空白字符分割命令

        if (parts.length == 0) {
            System.out.println("RMIClient error: The input is not standardized and the command cannot be empty.");
            return null;
        }

        String action = parts[0].toUpperCase(); // 将动作转换为大写

        switch (action) {
            case "PUT":
                if (parts.length != 3) {
                    System.out.println("RMIClient error: The input is not standardized，PUT needs two parameters.");
                    return null;
                }
                break;
            case "GET":
            case "DELETE":
                if (parts.length != 2) {
                    System.out.println("RMIClient error: The input is not standardized，GET needs one parameters.");
                    return null;
                }
                break;
            default:
                System.out.println("RMIClient error: The input is not standardized with " + action);
                return null;
        }

        // 构造并返回结果数组
        String[] result = new String[parts.length];
        result[0] = action;
        System.arraycopy(parts, 1, result, 1, parts.length - 1);
        return result;
    }
    /**
     * Calls a remote method based on the command parts.
     *
     * @param stub The remote object stub for the dictionary service.
     * @param commandParts The parts of the command to execute.
     * @return The result of the remote method call.
     */
    public static String callRemoteMethod(RMIInterface stub, String[] commandParts) {
        try {
            String action = commandParts[0];
            switch (action) {
                case "PUT":
                    return stub.put(commandParts[1], commandParts[2]);
                case "GET":
                    return stub.get(commandParts[1]);
                case "DELETE":
                    return stub.delete(commandParts[1]);
                default:
                    return "Unrecognized command: " + action;
            }
        } catch (RemoteException e) {
            e.printStackTrace(); // 打印完整的堆栈跟踪
            return "Remote exception occurred.";
        }
    }


}
