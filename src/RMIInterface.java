import java.rmi.Remote;
import java.rmi.RemoteException;
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
    public boolean prepareToOperation(String key, String value, String operation) throws RemoteException;

    void finallyGotCommitSoReallyUpdate(String key, String value, String operation)throws RemoteException;
}
