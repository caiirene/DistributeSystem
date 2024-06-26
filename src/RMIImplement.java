import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RMIImplement is the implementation of the RMIInterface for a dictionary service.
 */
public class RMIImplement extends UnicastRemoteObject implements RMIInterface {

    private ConcurrentHashMap<String, String> dictionary;

    @Override
    public void updateDictionary(Map<String, String> commonDictionary) throws RemoteException {
        // 更新本地字典
        dictionary.clear();
        dictionary.putAll(commonDictionary);
        System.out.println("Dictionary has been updated.");
    }
    public ConcurrentHashMap<String, String> getDictionary() {
        return dictionary;
    }
    private ConcurrentHashMap<String, ReentrantLock> locks;
    private RMIServer server;
    private int port;

    private int promiseNumber;
    private String currentAccepting;
    /**
     * Constructor for RMIImplement.
     *
     * @throws RemoteException If a remote method call fails.
     */
    public RMIImplement(RMIServer server, int port) throws RemoteException {
        super();
        dictionary = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
        this.server = server;
        this.port = port;
    }

    public String requestAPrepare() throws RemoteException {
        return server.prepare();
    }

    public String promise(int proposalNum){
        if (proposalNum<=promiseNumber){
            return "false";
        }
        this.promiseNumber = proposalNum;
        if (currentAccepting!=null) {
            return "true " + proposalNum + " " + currentAccepting;
        }
        return "true " + proposalNum;
    }

    public String accepted(int proposalNum, String command) throws RemoteException {
        if (proposalNum<promiseNumber){
            return "This sever in port "+port+" already promised to a higher proposal number "+promiseNumber;
        }

        String[] parts = command.split(" ");  // Split the command based on spaces
        if (parts.length != 3) {  // Ensure that the command has exactly three parts
            return proposalNum+"Invalid command format. Expected format: 'key value operation'";
        }

        this.currentAccepting = command;

        String key = parts[0];
        String value = parts[1];
        String operation = parts[2];
        if (!prepareToOperation(key,value,operation)){
            return proposalNum+"This sever in port "+port+" does not allow such operation";
        }

        finallyGotCommitSoReallyUpdate(key,value,operation);
        this.currentAccepting = null;

        return proposalNum+" done";
    }

    /**
     * Prepares the server for an operation (PUT or DELETE) by acquiring a lock and checking for conflicts.
     *
     * @param key The key to be put or deleted.
     * @param value The value to be associated with the key (ignored for DELETE operation).
     * @param operation The type of operation ("put" or "delete").
     * @return {@code true} if the operation can proceed (no conflicts and lock acquired),
     *         {@code false} otherwise.
     * @throws RemoteException if a remote communication error occurs.
     */
    public boolean prepareToOperation(String key, String value, String operation) throws RemoteException {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        if (lock.tryLock()) {
            // 检查是否存在冲突的更新
            if ("put".equals(operation) && dictionary.containsKey(key)) {
                System.out.println("port "+port+" already has the key, refuse to put.");
                lock.unlock();
                return false;
            } else if ("delete".equals(operation) && !dictionary.containsKey(key)) {
                System.out.println("port "+port+" has no such key, refuse to delete.");
                lock.unlock();
                return false;
            }
            return true;
        } else {
            System.out.println("port "+port+" because of some reason, maybe lock, can't "+operation+".");
            return false;
        }
    }
    /**
     * Commits an update to the dictionary by performing the actual PUT or DELETE operation,
     * and then releases the lock associated with the key.
     *
     * @param key The key to be put or deleted.
     * @param value The value to be associated with the key (ignored for DELETE operation).
     * @param operation The type of operation ("put" or "delete").
     * @throws RemoteException if a remote communication error occurs.
     */
    public void finallyGotCommitSoReallyUpdate(String key, String value, String operation) throws RemoteException {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                if ("put".equals(operation)) {
                    dictionary.put(key, value);
                } else if ("delete".equals(operation)) {
                    dictionary.remove(key);
                }
            } finally {
                lock.unlock();
            }
        }
    }
    /**
     * Puts a key-value pair into the dictionary.
     *
     * @param key The key to be added or updated.
     * @param value The value associated with the key.
     * @return A success message if the operation is successful, or an error message if the key already exists.
     * @throws RemoteException If a remote method call fails.
     */
    public String put(String key, String value) throws RemoteException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String responseForClient = null;

        try {
            //先自行判断一遍
            if (!prepareToOperation(key, value, "put")) {
                return "RMIImplement on port " + port + " error: Not allowed by self-checking. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
            }
            //第一步！！！！！！！！！！！！！先让总server做个prepare来回，如果总server提案号成功，则会返回一段话
            else {
                responseForClient = requestAPrepare();
                if (responseForClient.equals("didn't got promise from majority.")){
                    return "didn't got promise from majority.";
                }
                //第二步！！！！！！！！！！！！如果成功，进行第二步
                else if (responseForClient.equals("got promise success from most acceptors")) {
                    //如果返回的时候没有带specialMeassage，则直接使用自己从client那里拿到的参数作为指令
                    responseForClient = server.accept(promiseNumber,key+" "+value+" put");
                } else {
                    //如果带了specialMessage，则使用specialMessage作为指令
                    String specialMessage = responseForClient.split(":")[1];
                    responseForClient = server.accept(promiseNumber,specialMessage);
                }
                return responseForClient + sdf.format(new Date(System.currentTimeMillis()));
            }

        } catch (RemoteException e) {
            //e.printStackTrace();
            return "RMIImplement on port " + port + " error: RemoteException occurred. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
        }
    }

    /**
     * Retrieves the value associated with a key from the dictionary.
     *
     * @param key The key whose value is to be retrieved.
     * @return The value associated with the key, or an error message if the key is not found.
     * @throws RemoteException If a remote method call fails.
     */
    @Override
    public String get(String key) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        //System.out.println("Current time: " + sdf.format(new Date(System.currentTimeMillis())));
        if (!dictionary.containsKey(key)){
            return "RMIImplement get error: There is no such key."
                    + "Current time: " + sdf.format(new Date(System.currentTimeMillis()));
        }
        return "RMIImplement: GET success for key (" + key + ")" + " with value (" + dictionary.get(key) + ")"
                + "Current time: " + sdf.format(new Date(System.currentTimeMillis()));
    }
    /**
     * Deletes a key-value pair from the dictionary.
     *
     * @param key The key to be deleted.
     * @return A success message if the operation is successful, or an error message if the key is not found or already deleted.
     * @throws RemoteException If a remote method call fails.
     */
    @Override
    public String delete(String key) throws RemoteException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String responseForClient = null;

        try {
            // 第一步：先自行判断一遍
            if (!prepareToOperation(key, "", "delete")) {
                return "RMIImplement on port " + port + " delete error: Not allowed by self-checking. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
            }

            // 第二步：让总server做个prepare来回，如果总server提案号成功，则会返回一段话
            responseForClient = requestAPrepare();
            if (responseForClient.equals("didn't got promise from majority.")){
                return "didn't got promise from majority.";
            }

            // 第三步：如果成功，进行第二步
            else if (responseForClient.equals("got promise success from most acceptors")) {
                // 如果返回的时候没有带specialMessage，则直接使用自己从client那里拿到的参数作为指令
                responseForClient = server.accept(promiseNumber, key + " null delete");
            } else {
                // 如果带了specialMessage，则使用specialMessage作为指令
                String specialMessage = responseForClient.split(":")[1];
                responseForClient = server.accept(promiseNumber, specialMessage);
            }
            return responseForClient + sdf.format(new Date(System.currentTimeMillis()));
        } catch (RemoteException e) {
            // e.printStackTrace();
            return "RMIImplement on port " + port + " delete error: RemoteException occurred. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
        }
    }

}
