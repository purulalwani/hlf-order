package com.market.client;

import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * <h1>HFJavaSDKBasicExample</h1>
 * <p>
 * Simple example showcasing basic fabric-ca and fabric actions.
 * The demo required fabcar fabric up and running.
 * <p>
 * The demo shows
 * <ul>
 * <li>connecting to fabric-ca</li>
 * <li>enrolling admin to get new key-pair, certificate</li>
 * <li>registering and enrolling a new user using admin</li>
 * <li>creating HF client and initializing channel</li>
 * <li>invoking chaincode query</li>
 * </ul>
 *
 * @author lkolisko
 */
public class HFJavaSDKBasicExample {

    private static final Logger log = Logger.getLogger(HFJavaSDKBasicExample.class);


    public static void main(String[] args) throws Exception {
        // create fabric-ca client
        HFCAClient caClient = getHfCaClient("http://localhost:7054", null);

        // enroll or load admin
        AppUser admin = getAdmin(caClient);
        log.info(admin);

        // register and enroll new user
        AppUser appUser = getUser(caClient, admin, "hfuser");
        log.info(appUser);

        // get HFC client instance
        HFClient client = getHfClient();
        // set user context
        client.setUserContext(admin);

        // get HFC channel using the client
        Channel channel = getChannel(client);
        log.info("Channel: " + channel.getName());
        
        // add new car
        sendTransaction(client, channel);

        // call query blockchain example
        queryBlockChain(client);
    }


    /**
     * Invoke blockchain query
     *
     * @param client The HF Client
     * @throws ProposalException
     * @throws InvalidArgumentException
     */
    static String queryBlockChain(HFClient client) throws ProposalException, InvalidArgumentException {
        // get channel instance from client
        Channel channel = client.getChannel("channel");
        // create chaincode request
        QueryByChaincodeRequest qpr = client.newQueryProposalRequest();
        // build cc id providing the chaincode name. Version is omitted here.
        ChaincodeID fabcarCCId = ChaincodeID.newBuilder().setName("market-network").build();
        qpr.setChaincodeID(fabcarCCId);
        // CC function to be called
        qpr.setFcn("itemList");
        Collection<ProposalResponse> res = channel.queryByChaincode(qpr);
        // display response
        String ret = null;
        for (ProposalResponse pres : res) {
            String stringResponse = new String(pres.getChaincodeActionResponsePayload());
            log.info(stringResponse);
            ret = stringResponse;
        }
        
        return ret;
    }
    
    public static String sendTransaction(HFClient client, Channel channel) throws ProposalException, InvalidArgumentException, InterruptedException, ExecutionException, TimeoutException{
    	TransactionProposalRequest tpr = client.newTransactionProposalRequest();
    	ChaincodeID cid = ChaincodeID.newBuilder().setName("market-network").build();
    	tpr.setChaincodeID(cid);
    	tpr.setFcn("addItem");
    	tpr.setArgs(new String[]{"ITEM1", "Mobile", "10", "Submitted"});
    	Collection<ProposalResponse> responses = channel.sendTransactionProposal(tpr);
    	List<ProposalResponse> invalid = responses.stream().filter(r -> r.isInvalid()).collect(Collectors.toList());
    	if (!invalid.isEmpty()) {
    	    invalid.forEach(response -> {
    	        log.error(response.getMessage());
    	    });
    	    throw new RuntimeException("invalid response(s) found");
    	}
    	
//    	//TransactionEvent txEvent = channel.sendTransaction(responses).get(60, TimeUnit.SECONDS);
//    	if (txEvent.isValid()) {
//    	    log.info("Transacion tx: " + txEvent.getTransactionID() + " is completed.");
//    	} else {
//    	    log.error("Transaction tx: " + txEvent.getTransactionID() + " is invalid.");
//    	}
    	
    	CompletableFuture<TransactionEvent> futureEvent = channel.sendTransaction(responses);
    	
    	
    	
    	while(!futureEvent.isDone()){
    		log.info("Waiting for tx to commit....");
    	}
    	
    	TransactionEvent txEvent = futureEvent.get();
		if (txEvent.isValid()) {
    	    log.info("Transacion tx: " + txEvent.getTransactionID() + " is completed.");
    	    return "Transacion tx: " + txEvent.getTransactionID() + " is completed.";
    	} else {
    	    log.error("Transaction tx: " + txEvent.getTransactionID() + " is invalid.");
    	    return "Transacion tx: " + txEvent.getTransactionID() + " is invalid.";
    	    
    	}
		
    	
    	
    }

    /**
     * Initialize and get HF channel
     *
     * @param client The HFC client
     * @return Initialized channel
     * @throws InvalidArgumentException
     * @throws TransactionException
     */
    static Channel getChannel(HFClient client) throws InvalidArgumentException, TransactionException {
        // initialize channel
        // peer name and endpoint in fabcar network
        Peer peer = client.newPeer("peer0.Seller.market.com", "grpc://localhost:8051");
        // eventhub name and endpoint in fabcar network
        EventHub eventHub = client.newEventHub("eventhub01", "grpc://localhost:8053");
        // orderer name and endpoint in fabcar network
        Orderer orderer = client.newOrderer("orderer.market.com", "grpc://localhost:7050");
        // channel name in fabcar network
        Channel channel = client.newChannel("channel");
        channel.addPeer(peer);
        channel.addEventHub(eventHub);
        channel.addOrderer(orderer);
        channel.initialize();
        return channel;
    }

    /**
     * Create new HLF client
     *
     * @return new HLF client instance. Never null.
     * @throws CryptoException
     * @throws InvalidArgumentException
     */
    static HFClient getHfClient() throws Exception {
        // initialize default cryptosuite
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        // setup the client
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(cryptoSuite);
        return client;
    }


    /**
     * Register and enroll user with userId.
     * If AppUser object with the name already exist on fs it will be loaded and
     * registration and enrollment will be skipped.
     *
     * @param caClient  The fabric-ca client.
     * @param registrar The registrar to be used.
     * @param userId    The user id.
     * @return AppUser instance with userId, affiliation,mspId and enrollment set.
     * @throws Exception
     */
    static AppUser getUser(HFCAClient caClient, AppUser registrar, String userId) throws Exception {
        AppUser appUser = tryDeserialize(userId);
        if (appUser == null) {
            RegistrationRequest rr = new RegistrationRequest(userId, "org1");
            String enrollmentSecret = caClient.register(rr, registrar);
            Enrollment enrollment = caClient.enroll(userId, enrollmentSecret);
            appUser = new AppUser(userId, "org1", "SellerMSP", enrollment);
            serialize(appUser);
        }
        return appUser;
    }

    /**
     * Enroll admin into fabric-ca using {@code admin/adminpw} credentials.
     * If AppUser object already exist serialized on fs it will be loaded and
     * new enrollment will not be executed.
     *
     * @param caClient The fabric-ca client
     * @return AppUser instance with userid, affiliation, mspId and enrollment set
     * @throws Exception
     */
    static AppUser getAdmin(HFCAClient caClient) throws Exception {
        AppUser admin = tryDeserialize("admin");
        if (admin == null) {
            Enrollment adminEnrollment = caClient.enroll("admin", "adminpw");
            admin = new AppUser("admin", "org1", "SellerMSP", adminEnrollment);
            serialize(admin);
        }
        return admin;
    }

    /**
     * Get new fabic-ca client
     *
     * @param caUrl              The fabric-ca-server endpoint url
     * @param caClientProperties The fabri-ca client properties. Can be null.
     * @return new client instance. never null.
     * @throws Exception
     */
    static HFCAClient getHfCaClient(String caUrl, Properties caClientProperties) throws Exception {
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        HFCAClient caClient = HFCAClient.createNewInstance(caUrl, caClientProperties);
        caClient.setCryptoSuite(cryptoSuite);
        return caClient;
    }


    // user serialization and deserialization utility functions
    // files are stored in the base directory

    /**
     * Serialize AppUser object to file
     *
     * @param appUser The object to be serialized
     * @throws IOException
     */
    static void serialize(AppUser appUser) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(
                Paths.get(appUser.getName() + ".jso")))) {
            oos.writeObject(appUser);
        }
    }

    /**
     * Deserialize AppUser object from file
     *
     * @param name The name of the user. Used to build file name ${name}.jso
     * @return
     * @throws Exception
     */
    static AppUser tryDeserialize(String name) throws Exception {
        if (Files.exists(Paths.get(name + ".jso"))) {
            return deserialize(name);
        }
        return null;
    }

    static AppUser deserialize(String name) throws Exception {
        try (ObjectInputStream decoder = new ObjectInputStream(
                Files.newInputStream(Paths.get(name + ".jso")))) {
            return (AppUser) decoder.readObject();
        }
    }
    
    //// for Rest APIs
    
    public static String createItem(Item item) throws Exception {
        // create fabric-ca client
        HFCAClient caClient = getHfCaClient("http://localhost:7054", null);

        // enroll or load admin
        AppUser admin = getAdmin(caClient);
        log.info(admin);

        // register and enroll new user
        AppUser appUser = getUser(caClient, admin, "hfuser");
        log.info(appUser);

        // get HFC client instance
        HFClient client = getHfClient();
        // set user context
        client.setUserContext(admin);

        // get HFC channel using the client
        Channel channel = getChannel(client);
        log.info("Channel: " + channel.getName());
        
        // add new car
        return sendTransaction(client, channel, item);

    }
    
    private static String sendTransaction(HFClient client, Channel channel, Item item) throws ProposalException, InvalidArgumentException, InterruptedException, ExecutionException {
    	System.out.println("Send Transaction Car ID -> " + item.itemId);
    	TransactionProposalRequest tpr = client.newTransactionProposalRequest();
    	ChaincodeID cid = ChaincodeID.newBuilder().setName("market-network").build();
    	tpr.setChaincodeID(cid);
    	tpr.setFcn("addItem");
    	tpr.setArgs(new String[]{item.itemId, item.itemPrice, item.itemQuantity, item.itemStatus});
    	Collection<ProposalResponse> responses = channel.sendTransactionProposal(tpr);
    	List<ProposalResponse> invalid = responses.stream().filter(r -> r.isInvalid()).collect(Collectors.toList());
    	if (!invalid.isEmpty()) {
    	    invalid.forEach(response -> {
    	        log.error(response.getMessage());
    	    });
    	    throw new RuntimeException("invalid response(s) found");
    	}
    	
//    	//TransactionEvent txEvent = channel.sendTransaction(responses).get(60, TimeUnit.SECONDS);
//    	if (txEvent.isValid()) {
//    	    log.info("Transacion tx: " + txEvent.getTransactionID() + " is completed.");
//    	} else {
//    	    log.error("Transaction tx: " + txEvent.getTransactionID() + " is invalid.");
//    	}
    	
    	CompletableFuture<TransactionEvent> futureEvent = channel.sendTransaction(responses);
    	
    	
    	
    	while(!futureEvent.isDone()){
    		log.info("Waiting for tx to commit....");
    	}
    	
    	TransactionEvent txEvent = futureEvent.get();
		if (txEvent.isValid()) {
    	    log.info("Transacion tx: " + txEvent.getTransactionID() + " is completed.");
    	    return "Transacion tx: " + txEvent.getTransactionID() + " is completed.";
    	} else {
    	    log.error("Transaction tx: " + txEvent.getTransactionID() + " is invalid.");
    	    return "Transacion tx: " + txEvent.getTransactionID() + " is invalid.";
    	    
    	}

	}


	public static String itemList() throws Exception {
        // create fabric-ca client
        HFCAClient caClient = getHfCaClient("http://localhost:7054", null);

        // enroll or load admin
        AppUser admin = getAdmin(caClient);
        log.info(admin);

        // register and enroll new user
        AppUser appUser = getUser(caClient, admin, "hfuser");
        log.info(appUser);

        // get HFC client instance
        HFClient client = getHfClient();
        // set user context
        client.setUserContext(admin);

        // get HFC channel using the client
        Channel channel = getChannel(client);
        log.info("Channel: " + channel.getName());
        
        

        // call query blockchain example
        return queryBlockChain(client);
    }


}
