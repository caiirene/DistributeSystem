import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

/**
 * RMIServer is the server for the RMI dictionary service.
 */
public class RMIServer {

    private ArrayList<RMIInterface> serverStubs;

    /**
     * constructor
     */
    public RMIServer() {
        serverStubs = new ArrayList<>();
    }

    /**
     * The main method that starts the RMIServer.
     *
     * @param args Command line arguments (not used in this implementation).
     */
    public static void main(String[] args) {
        RMIServer server = new RMIServer(); // 创建 RMIServer 实例
        server.startServers(); // 启动服务器
    }
    /**
     * Starts multiple RMI server instances, each on a different port, and binds remote objects to the RMI registry.
     */
    public void startServers() {
        int port = 2000;
        for (int i = 0; i < 5; i++) {
            try {
                RMIInterface obj = new RMIImplement(this, port); // 使用 server 实例
                java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.createRegistry(port);
                registry.bind("RMIDictionary" + i, obj);
                System.out.println("RMIServer: Server ready on port " + port);
                serverStubs.add(obj); // 将远程对象引用添加到列表中
                port++;
            } catch (Exception e) {
                System.out.println("RMIServer exception: can't start the server.\n" + e.toString());
            }
        }
    }
    /**
     * Asks all server instances whether they can accept a proposed operation (PUT or DELETE).
     *
     * @param key The key to be put or deleted.
     * @param value The value to be associated with the key (ignored for DELETE operation).
     * @param operation The type of operation ("put" or "delete").
     * @return {@code true} if all server instances agree to the update,
     *         {@code false} otherwise (if any server instance disagrees or cannot be contacted).
     */
    public boolean askAllServer(String key, String value, String operation) {
        for (RMIInterface stub : serverStubs) {
            try {
                if (!stub.prepareToOperation(key, value, operation)) {
                    return false;
                }
            } catch (RemoteException e) {
                System.err.println("RMIServer Error contacting server: " + e.getMessage());
                return false; // 如果无法联系服务器，也返回 false
            }
        }
        return true; // 所有服务器实例都同意更新
    }
    /**
     * Instructs all server instances to commit the actual update (PUT or DELETE) to their dictionaries.
     *
     * @param key The key to be put or deleted.
     * @param value The value to be associated with the key (ignored for DELETE operation).
     * @param operation The type of operation ("put" or "delete").
     */
    public void allServerDoRealUpdate(String key, String value, String operation) {
        for (RMIInterface stub : serverStubs) {
            try {
                stub.finallyGotCommitSoReallyUpdate(key,value,operation);
                }
            catch (RemoteException e) {
                System.err.println("RMIServer Error contacting server: " + e.getMessage());
            }
        }
    }
}
