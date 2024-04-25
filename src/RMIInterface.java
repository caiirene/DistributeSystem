import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RMIInterface defines the methods available for the remote dictionary service.
 */
public interface RMIInterface extends Remote {
    /**
     * Puts a key-value pair into the dictionary.
     *
     * @param key The key to be added or updated.
     * @param value The value associated with the key.
     * @return A success message if the operation is successful, or an error message if the key already exists.
     * @throws RemoteException If a remote method call fails.
     */
    String put(String key, String value) throws RemoteException;
    /**
     * Retrieves the value associated with a key from the dictionary.
     *
     * @param key The key whose value is to be retrieved.
     * @return The value associated with the key, or an error message if the key is not found.
     * @throws RemoteException If a remote method call fails.
     */
    String get(String key) throws RemoteException;
    /**
     * Deletes a key-value pair from the dictionary.
     *
     * @param key The key to be deleted.
     * @return A success message if the operation is successful, or an error message if the key is not found or already deleted.
     * @throws RemoteException If a remote method call fails.
     */
    String delete(String key) throws RemoteException;
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
    public boolean prepareToOperation(String key, String value, String operation) throws RemoteException;
    /**
     * Commits an update to the dictionary by performing the actual PUT or DELETE operation,
     * and then releases the lock associated with the key.
     *
     * @param key The key to be put or deleted.
     * @param value The value to be associated with the key (ignored for DELETE operation).
     * @param operation The type of operation ("put" or "delete").
     * @throws RemoteException if a remote communication error occurs.
     */
    void finallyGotCommitSoReallyUpdate(String key, String value, String operation)throws RemoteException;

    String promise(int proposalNumber) throws RemoteException;

    String accepted(int promiseNum, String val) throws RemoteException;


    ConcurrentHashMap<String, String> getDictionary()throws RemoteException;;

    void updateDictionary(Map<String, String> commonDictionary) throws RemoteException;
}
