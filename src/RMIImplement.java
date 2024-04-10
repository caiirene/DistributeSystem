import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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
    private ConcurrentHashMap<String, ReentrantLock> locks;
    private RMIServer server;
    private int port;


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


//    public boolean prepareToOperation(String key, String value, String operation) throws RemoteException {
//        System.out.println("RMIImplement进入prepareToOperation函数。");
//        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
//        System.out.println("prepareToOperation函数创建锁成功或者之前就有锁。"+lock);
//        if (lock.tryLock()) {
//            System.out.println("RMIImplement进入prepareToOperation函数的if语句。");
//            try {
//                System.out.println("RMIImplement进入prepareToOperation函数的try语句。");
//                // 检查是否存在冲突的更新
//                if ("put".equals(operation) && dictionary.containsKey(key)) {
//                    System.out.println("RMIImplement进入prepareToOperation函数检测到put冲突在端口："+port);
//                    return false;
//                } else if ("delete".equals(operation) && !dictionary.containsKey(key)) {
//                    System.out.println("RMIImplement进入prepareToOperation函数检测到delete冲突在端口："+port);
//                    return false;
//                }
//                System.out.println("RMIImplement进入prepareToOperation函数没有冲突在端口："+port);
//                // 其他检查逻辑...
//                return true; // 接受更新
//            } finally {
//                System.out.println("RMIImplement进入prepareToOperation函数的finally语句在端口："+port);
//                // 如果不接受更新，需要释放锁
//                if (!dictionary.containsKey(key) || !"PUT".equals(operation)) {
//                    lock.unlock();
//                    System.out.println("RMIImplement进入prepareToOperation函数释放锁，端口："+port);
//                }
//            }
//        } else {
//            System.out.println("RMIImplement进入prepareToOperation函数的最终else语句在端口："+port);
//            return false; // 无法获取锁，拒绝更新
//        }
//    }
    public boolean prepareToOperation(String key, String value, String operation) throws RemoteException {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        if (lock.tryLock()) {
            System.out.println("端口"+port+"进入if (lock.tryLock())语句。");
            // 检查是否存在冲突的更新
            if ("put".equals(operation) && dictionary.containsKey(key)) {
                System.out.println("端口"+port+"进入if (\"PUT\".equals(operation) && dictionary.containsKey(key))语句。");
                System.out.println("端口"+port+"已经存在相同key，不能put。");
                // 如果是 PUT 操作，并且键已经存在，可能存在冲突
                lock.unlock(); // 如果不接受更新，需要释放锁
                return false; // 拒绝更新
            } else if ("delete".equals(operation) && !dictionary.containsKey(key)) {
                System.out.println("端口"+port+"进入if (\"DELETE\".equals(operation) && dictionary.containsKey(key))语句。");
                // 如果是 DELETE 操作，并且键不存在，拒绝更新
                lock.unlock(); // 如果不接受更新，需要释放锁
                return false;
            }
            System.out.println("端口"+port+"接受put"+key+"："+value);
            // 其他检查逻辑...
            return true; // 接受更新
        } else {
            System.out.println("端口"+port+"因其他原因，可能是lock，不能put。");
            return false; // 无法获取锁，拒绝更新
        }
    }



    public void finallyGotCommitSoReallyUpdate(String key, String value, String operation) throws RemoteException {
        System.out.println("端口"+port+"开始执行finallyGotCommitSoReallyUpdate函数。");
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            System.out.println("端口"+port+"finallyGotCommitSoReallyUpdate函数进入if语句。");
            try {
                System.out.println("端口"+port+"finallyGotCommitSoReallyUpdate函数进入try语句。");
                if ("put".equals(operation)) {
                    System.out.println("端口"+port+"finallyGotCommitSoReallyUpdate函数进入第二个if put语句。");
                    dictionary.put(key, value);
                } else if ("delete".equals(operation)) {
                    System.out.println("端口"+port+"finallyGotCommitSoReallyUpdate函数进入第二个else if delete语句。");
                    dictionary.remove(key);
                }
            } finally {
                System.out.println("端口"+port+"finallyGotCommitSoReallyUpdate函数进入finally语句。");
                lock.unlock();
            }
        }
        System.out.println("端口"+port+"finallyGotCommitSoReallyUpdate函数结束。");
        System.out.println("------------------"+"\n"+
                "端口"+port+"现在字典状态"+dictionary);
    }

    /**
     * Puts a key-value pair into the dictionary.
     *
     * @param key The key to be added or updated.
     * @param value The value associated with the key.
     * @return A success message if the operation is successful, or an error message if the key already exists.
     * @throws RemoteException If a remote method call fails.
     */
    @Override
//    public String put(String key, String value) throws RemoteException{
//        System.out.println("RMIImplement开始调用put方法。");
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//        if (!prepareToOperation(key, value, "put"))   {
//            System.out.println("RMIImplement on port "+port+" error: Not allowed by self-checking."
//                    + "Current time: " + sdf.format(new Date(System.currentTimeMillis())));
//            return "RMIImplement on port "+port+" error: Not allowed by self-checking."
//                    + "Current time: " + sdf.format(new Date(System.currentTimeMillis()));
//        } else if (!server.askAllServer(key, value, "put")) {
//            System.out.println("RMIImplement on port "+port+" error: Not allowed by other server."
//                    + "Current time: " + sdf.format(new Date(System.currentTimeMillis())));
//            return "RMIImplement on port "+port+" error: Not allowed by other server."
//                    + "Current time: " + sdf.format(new Date(System.currentTimeMillis()));
//        }
//        finallyGotCommitSoReallyUpdate(key, value, "put");
//
//        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//        System.out.println("RMIImplement: PUT success for key (" + key + ")"
//                + "Current time: " + sdf.format(new Date(System.currentTimeMillis())));
//        return "RMIImplement: PUT success for key (" + key + ")"
//                + "Current time: " + sdf.format(new Date(System.currentTimeMillis()));
//    }
    public String put(String key, String value) throws RemoteException {
        System.out.println("RMIImplement开始调用put方法在端口："+port);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            System.out.println("进入try语句。");
            if (!prepareToOperation(key, value, "put")) {
                System.out.println("进入if语句。");
                System.out.println("RMIImplement on port " + port + " error: Not allowed by self-checking. Current time: " + sdf.format(new Date(System.currentTimeMillis())));
                return "RMIImplement on port " + port + " error: Not allowed by self-checking. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
            } else if (!server.askAllServer(key, value, "put")) {
                System.out.println("进入else if语句。");
                System.out.println("RMIImplement on port " + port + " error: Not allowed by other server. Current time: " + sdf.format(new Date(System.currentTimeMillis())));
                return "RMIImplement on port " + port + " error: Not allowed by other server. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
            }
            System.out.println("没有进入if或者else if语句，马上开始执行finallyGotCommitSoReallyUpdate方法。");
            server.allServerDoRealUpdate(key, value, "put");

        } catch (RemoteException e) {
            System.out.println("进入catch语句。");
            e.printStackTrace();
            return "RMIImplement on port " + port + " error: RemoteException occurred. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
        }
        System.out.println("跳出try-catch语句。");

        System.out.println("RMIImplement: PUT success for key (" + key + ") Current time: " + sdf.format(new Date(System.currentTimeMillis())));
        return "RMIImplement: PUT success for key (" + key + ") Current time: " + sdf.format(new Date(System.currentTimeMillis()));
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
        //return "RMIImplement: GET success for key (" + key + ")" + " with value (" + dictionary.get(key) + ")";
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
        System.out.println("RMIImplement开始调用delete方法在端口：" + port);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            System.out.println("进入try语句。");
            if (!prepareToOperation(key, "", "delete")) {
                System.out.println("进入if语句。");
                System.out.println("RMIImplement on port " + port + " delete error: Not allowed by self-checking. Current time: " + sdf.format(new Date(System.currentTimeMillis())));
                return "RMIImplement on port " + port + " delete error: Not allowed by self-checking. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
            } else if (!server.askAllServer(key, "", "delete")) {
                System.out.println("进入else if语句。");
                System.out.println("RMIImplement on port " + port + " delete error: Not allowed by other server. Current time: " + sdf.format(new Date(System.currentTimeMillis())));
                return "RMIImplement on port " + port + " delete error: Not allowed by other server. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
            }
            System.out.println("没有进入if或者else if语句，马上开始执行finallyGotCommitSoReallyUpdate方法。");
            server.allServerDoRealUpdate(key, "", "delete");

        } catch (RemoteException e) {
            System.out.println("进入catch语句。");
            e.printStackTrace();
            return "RMIImplement on port " + port + " delete error: RemoteException occurred. Current time: " + sdf.format(new Date(System.currentTimeMillis()));
        }
        System.out.println("跳出try-catch语句。");

        System.out.println("RMIImplement: DELETE success for key (" + key + ") Current time: " + sdf.format(new Date(System.currentTimeMillis())));
        return "RMIImplement: DELETE success for key (" + key + ") Current time: " + sdf.format(new Date(System.currentTimeMillis()));
    }

}
