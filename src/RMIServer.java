import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.rmi.RemoteException;

/**
 * RMIServer is the server for the RMI dictionary service.
 */
public class RMIServer {

    private ArrayList<RMIInterface> serverStubs;
    private int proposalNumber = 1;
    private HashMap<RMIInterface, Integer> portStubMap = new HashMap<>();

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
                portStubMap.put(obj, port);  // 存储stub和端口的映射
                serverStubs.add(obj); // 将远程对象引用添加到列表中
                port++;
            } catch (Exception e) {
                System.out.println("RMIServer exception: can't start the server.\n" + e.toString());
            }
        }
        startHeartbeat();
    }
    public String prepare() throws RemoteException {
        String responseForClient = null;
        int proposalNumber = this.proposalNumber;
        this.proposalNumber++;
        int countPromises = 0;  // 用于计数承诺的数量
        int totalStubs = serverStubs.size();  // 总的实例数量
        String promiseInfoFromStub;
        promiseInfoFromStub = null;

        String specialMessage = null;

        for (RMIInterface stub : serverStubs) {
            // 发送 prepare 请求并获取响应
            String response = stub.promise(proposalNumber);
            String[] responseParts = response.split(" ");  // 解析响应

            if (responseParts[0].equals("false")){
                continue;
            } else if (responseParts[0].equals("true") && (responseParts.length == 2)) {
                countPromises++;
            } else if (responseParts[0].equals("true") && (responseParts.length == 4)) {
                countPromises++;
                specialMessage = responseParts[3];
            } else {
                specialMessage = "something wrong";
            }

        }
        if ((countPromises > totalStubs / 2) && specialMessage==null){
            responseForClient = "got promise success from most acceptors";
        } else if (countPromises <= totalStubs / 2) {
            responseForClient = "didn't got promise from majority.";
        } else {
            responseForClient = "got promise success from most acceptors, but need to change to:"+specialMessage;
        }

        // 判断是否获得了大多数的承诺
        return responseForClient;
    }

    public String accept(int promisedNum, String command) throws RemoteException {
        StringBuilder responseForClient = new StringBuilder();
        int acceptCount = 0; // 计数器，用于记录成功接受并完成的服务器数量

        for (RMIInterface stub : serverStubs) {
            try {
                // Assuming command is a string like "key value operation"
                String response = stub.accepted(promisedNum, command);
                if (response.endsWith(" done")) { // 检查服务器返回的响应是否以 "数字 done" 结尾
                    acceptCount++; // 如果是，计数+1
                }
                responseForClient.append(" ").append(response);
            } catch (RemoteException e) {
                responseForClient.append(" Error contacting server: ").append(e.getMessage());
            }
        }

        // 如果接受的服务器数量达到或超过3个，执行学习
        if (acceptCount >= 3) {
            learn(promisedNum, command);
        }

        return responseForClient.toString();
    }

    private ConcurrentHashMap<Integer, String> learnedProposals = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> commonDictionary = new ConcurrentHashMap<>();

    // Learner 学习提案的方法
    private void learn(int proposalNum, String proposalValue) {
        // 存储提案
        learnedProposals.put(proposalNum, proposalValue);

        // 解析提案值
        String[] parts = proposalValue.split(" ");
        if (parts.length != 3) {
            System.out.println(proposalNum + " Invalid command format. Expected format: 'key value operation'");
            return;
        }

        String key = parts[0];
        String value = parts[1];
        String operation = parts[2];

        // 根据操作类型更新字典
        switch (operation.toLowerCase()) {
            case "put":
                commonDictionary.put(key, value);
                System.out.println("Performed PUT operation: Key = " + key + ", Value = " + value);
                break;
            case "delete":
                if (commonDictionary.containsKey(key)) {
                    commonDictionary.remove(key);
                    System.out.println("Performed DELETE operation: Key = " + key);
                } else {
                    System.out.println("Attempted to DELETE non-existent key: " + key);
                }
                break;
            default:
                System.out.println("Unknown operation '" + operation + "' in proposal #" + proposalNum);
                break;
        }

        // 打印已学习提案的信息
        System.out.println("Learned proposal #" + proposalNum + " with value: " + proposalValue);
    }

    private Timer heartbeatTimer;
    private void startHeartbeat() {
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                portStubMap.forEach((stub, port) -> {
                    try {
                        ConcurrentHashMap<String, String> remoteDictionary = stub.getDictionary();
                        if (!remoteDictionary.equals(commonDictionary)) {
                            stub.updateDictionary(commonDictionary);
                            System.out.println("Updated dictionary on server at port " + port);
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        System.out.println("心跳测试 "+sdf.format(new Date(System.currentTimeMillis())));
                    } catch (RemoteException e) {
                        System.out.println("Failed to contact server on port " + port + ", attempting to reconnect.");
                        reconnectStub(stub, port);
                    }
                });
            }
        }, 0, 10000); // 每隔5秒执行一次
    }

    private void reconnectStub(RMIInterface stub, int port) {
        try {
            RMIInterface obj = new RMIImplement(this,port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("RMIDictionary", obj);
            portStubMap.put(obj, port);  // 更新映射
            System.out.println("Reconnected to server on port " + port);
        } catch (Exception e) {
            System.out.println("Failed to reconnect to server on port " + port + ". " + e.getMessage());
        }
    }


}
