package com.fisco.app.contract.bill;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Address;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray;
import org.fisco.bcos.sdk.v3.codec.datatypes.Event;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple4;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.eventsub.EventSubCallback;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class BillMergeV2 extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b50610e76806100206000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c80632ce3cad214610051578063b6d0b4871461006d578063cfde1527146100a0578063d8c45889146100d0575b600080fd5b61006b60048036038101906100669190610844565b610100565b005b610087600480360381019061008291906108c7565b61042a565b6040516100979493929190610a02565b60405180910390f35b6100ba60048036038101906100b591906108c7565b61053e565b6040516100c79190610a5d565b60405180910390f35b6100ea60048036038101906100e591906108c7565b61055b565b6040516100f79190610a78565b60405180910390f35b6000801b831415610146576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161013d90610af7565b60405180910390fd5b60028451101561018b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161018290610b63565b60405180910390fd5b60008084815260200190815260200160002060040160159054906101000a900460ff16156101ee576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016101e590610bcf565b60405180910390fd5b60005b84518110156102e357600085828151811061020f5761020e610bef565b5b602002602001015190506000801b81141561025f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161025690610c6a565b60405180910390fd5b6000801b6001600083815260200190815260200160002054146102b7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102ae90610cd6565b60405180910390fd5b8460016000838152602001908152602001600020819055505080806102db90610d25565b9150506101f1565b5060008060008581526020019081526020016000209050848160000190805190602001906103129291906105c8565b50838160010181905550828160020181905550428160030181905550338160040160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508160ff16600181111561038757610386610d6e565b5b8160040160146101000a81548160ff021916908360018111156103ad576103ac610d6e565b5b021790555060018160040160156101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff16847f70b74c0e68ad7ab46ce20e5f5b105188cd23ac66acb2461b560ed2a6b5fe94708751864260405161041b93929190610d9d565b60405180910390a35050505050565b6000806000606060008086815260200190815260200160002060040160159054906101000a900460ff16610493576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161048a90610e20565b60405180910390fd5b60008060008781526020019081526020016000209050806002015481600301548260040160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16836000018080548060200260200160405190810160405280929190818152602001828054801561052757602002820191906000526020600020905b815481526020019060010190808311610513575b505050505090509450945094509450509193509193565b600060016000838152602001908152602001600020549050919050565b60606000808381526020019081526020016000206000018054806020026020016040519081016040528092919081815260200182805480156105bc57602002820191906000526020600020905b8154815260200190600101908083116105a8575b50505050509050919050565b828054828255906000526020600020908101928215610604579160200282015b828111156106035782518255916020019190600101906105e8565b5b5090506106119190610615565b5090565b5b8082111561062e576000816000905550600101610616565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6106948261064b565b810181811067ffffffffffffffff821117156106b3576106b261065c565b5b80604052505050565b60006106c6610632565b90506106d2828261068b565b919050565b600067ffffffffffffffff8211156106f2576106f161065c565b5b602082029050602081019050919050565b600080fd5b6000819050919050565b61071b81610708565b811461072657600080fd5b50565b60008135905061073881610712565b92915050565b600061075161074c846106d7565b6106bc565b9050808382526020820190506020840283018581111561077457610773610703565b5b835b8181101561079d57806107898882610729565b845260208401935050602081019050610776565b5050509392505050565b600082601f8301126107bc576107bb610646565b5b81356107cc84826020860161073e565b91505092915050565b6000819050919050565b6107e8816107d5565b81146107f357600080fd5b50565b600081359050610805816107df565b92915050565b600060ff82169050919050565b6108218161080b565b811461082c57600080fd5b50565b60008135905061083e81610818565b92915050565b6000806000806080858703121561085e5761085d61063c565b5b600085013567ffffffffffffffff81111561087c5761087b610641565b5b610888878288016107a7565b945050602061089987828801610729565b93505060406108aa878288016107f6565b92505060606108bb8782880161082f565b91505092959194509250565b6000602082840312156108dd576108dc61063c565b5b60006108eb84828501610729565b91505092915050565b6108fd816107d5565b82525050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b600061092e82610903565b9050919050565b61093e81610923565b82525050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b61097981610708565b82525050565b600061098b8383610970565b60208301905092915050565b6000602082019050919050565b60006109af82610944565b6109b9818561094f565b93506109c483610960565b8060005b838110156109f55781516109dc888261097f565b97506109e783610997565b9250506001810190506109c8565b5085935050505092915050565b6000608082019050610a1760008301876108f4565b610a2460208301866108f4565b610a316040830185610935565b8181036060830152610a4381846109a4565b905095945050505050565b610a5781610708565b82525050565b6000602082019050610a726000830184610a4e565b92915050565b60006020820190508181036000830152610a9281846109a4565b905092915050565b600082825260208201905092915050565b7f496e76616c6964206e6577206173736574206861736800000000000000000000600082015250565b6000610ae1601683610a9a565b9150610aec82610aab565b602082019050919050565b60006020820190508181036000830152610b1081610ad4565b9050919050565b7f4d757374206d65726765206174206c6561737420322061737365747300000000600082015250565b6000610b4d601c83610a9a565b9150610b5882610b17565b602082019050919050565b60006020820190508181036000830152610b7c81610b40565b9050919050565b7f4e6577206173736574206861736820636f6c6c6973696f6e0000000000000000600082015250565b6000610bb9601883610a9a565b9150610bc482610b83565b602082019050919050565b60006020820190508181036000830152610be881610bac565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052603260045260246000fd5b7f496e76616c696420706172656e74206861736800000000000000000000000000600082015250565b6000610c54601383610a9a565b9150610c5f82610c1e565b602082019050919050565b60006020820190508181036000830152610c8381610c47565b9050919050565b7f506172656e7420617373657420616c7265616479206d65726765640000000000600082015250565b6000610cc0601b83610a9a565b9150610ccb82610c8a565b602082019050919050565b60006020820190508181036000830152610cef81610cb3565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610d30826107d5565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff821415610d6357610d62610cf6565b5b600182019050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602160045260246000fd5b6000606082019050610db260008301866108f4565b610dbf60208301856108f4565b610dcc60408301846108f4565b949350505050565b7f5265636f7264206e6f7420666f756e6400000000000000000000000000000000600082015250565b6000610e0a601083610a9a565b9150610e1582610dd4565b602082019050919050565b60006020820190508181036000830152610e3981610dfd565b905091905056fea264697066735822122050d208f517a10a1d2793f21c6f7316703d9ab4083bb815d01526b80d5b80673864736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b50610e76806100206000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c80631de847d914610051578063494596fc1461006d5780635499c254146100a0578063ccbdbbb6146100d0575b600080fd5b61006b60048036038101906100669190610844565b610100565b005b610087600480360381019061008291906108c7565b61042a565b6040516100979493929190610a02565b60405180910390f35b6100ba60048036038101906100b591906108c7565b61053e565b6040516100c79190610a4e565b60405180910390f35b6100ea60048036038101906100e591906108c7565b6105ab565b6040516100f79190610a7f565b60405180910390f35b6000801b831415610146576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161013d90610af7565b60405180910390fd5b60028451101561018b576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161018290610b63565b60405180910390fd5b60008084815260200190815260200160002060040160159054906101000a900460ff16156101ee576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004016101e590610bcf565b60405180910390fd5b60005b84518110156102e357600085828151811061020f5761020e610bef565b5b602002602001015190506000801b81141561025f576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161025690610c6a565b60405180910390fd5b6000801b6001600083815260200190815260200160002054146102b7576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004016102ae90610cd6565b60405180910390fd5b8460016000838152602001908152602001600020819055505080806102db90610d25565b9150506101f1565b5060008060008581526020019081526020016000209050848160000190805190602001906103129291906105c8565b50838160010181905550828160020181905550428160030181905550338160040160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508160ff16600181111561038757610386610d6e565b5b8160040160146101000a81548160ff021916908360018111156103ad576103ac610d6e565b5b021790555060018160040160156101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff16847f80f29a97d4b4311213b68d61f489a1076135ad17a24717cc2546692ea8560db98751864260405161041b93929190610d9d565b60405180910390a35050505050565b6000806000606060008086815260200190815260200160002060040160159054906101000a900460ff16610493576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161048a90610e20565b60405180910390fd5b60008060008781526020019081526020016000209050806002015481600301548260040160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16836000018080548060200260200160405190810160405280929190818152602001828054801561052757602002820191906000526020600020905b815481526020019060010190808311610513575b505050505090509450945094509450509193509193565b606060008083815260200190815260200160002060000180548060200260200160405190810160405280929190818152602001828054801561059f57602002820191906000526020600020905b81548152602001906001019080831161058b575b50505050509050919050565b600060016000838152602001908152602001600020549050919050565b828054828255906000526020600020908101928215610604579160200282015b828111156106035782518255916020019190600101906105e8565b5b5090506106119190610615565b5090565b5b8082111561062e576000816000905550600101610616565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6106948261064b565b810181811067ffffffffffffffff821117156106b3576106b261065c565b5b80604052505050565b60006106c6610632565b90506106d2828261068b565b919050565b600067ffffffffffffffff8211156106f2576106f161065c565b5b602082029050602081019050919050565b600080fd5b6000819050919050565b61071b81610708565b811461072657600080fd5b50565b60008135905061073881610712565b92915050565b600061075161074c846106d7565b6106bc565b9050808382526020820190506020840283018581111561077457610773610703565b5b835b8181101561079d57806107898882610729565b845260208401935050602081019050610776565b5050509392505050565b600082601f8301126107bc576107bb610646565b5b81356107cc84826020860161073e565b91505092915050565b6000819050919050565b6107e8816107d5565b81146107f357600080fd5b50565b600081359050610805816107df565b92915050565b600060ff82169050919050565b6108218161080b565b811461082c57600080fd5b50565b60008135905061083e81610818565b92915050565b6000806000806080858703121561085e5761085d61063c565b5b600085013567ffffffffffffffff81111561087c5761087b610641565b5b610888878288016107a7565b945050602061089987828801610729565b93505060406108aa878288016107f6565b92505060606108bb8782880161082f565b91505092959194509250565b6000602082840312156108dd576108dc61063c565b5b60006108eb84828501610729565b91505092915050565b6108fd816107d5565b82525050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b600061092e82610903565b9050919050565b61093e81610923565b82525050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b61097981610708565b82525050565b600061098b8383610970565b60208301905092915050565b6000602082019050919050565b60006109af82610944565b6109b9818561094f565b93506109c483610960565b8060005b838110156109f55781516109dc888261097f565b97506109e783610997565b9250506001810190506109c8565b5085935050505092915050565b6000608082019050610a1760008301876108f4565b610a2460208301866108f4565b610a316040830185610935565b8181036060830152610a4381846109a4565b905095945050505050565b60006020820190508181036000830152610a6881846109a4565b905092915050565b610a7981610708565b82525050565b6000602082019050610a946000830184610a70565b92915050565b600082825260208201905092915050565b7f496e76616c6964206e6577206173736574206861736800000000000000000000600082015250565b6000610ae1601683610a9a565b9150610aec82610aab565b602082019050919050565b60006020820190508181036000830152610b1081610ad4565b9050919050565b7f4d757374206d65726765206174206c6561737420322061737365747300000000600082015250565b6000610b4d601c83610a9a565b9150610b5882610b17565b602082019050919050565b60006020820190508181036000830152610b7c81610b40565b9050919050565b7f4e6577206173736574206861736820636f6c6c6973696f6e0000000000000000600082015250565b6000610bb9601883610a9a565b9150610bc482610b83565b602082019050919050565b60006020820190508181036000830152610be881610bac565b9050919050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052603260045260246000fd5b7f496e76616c696420706172656e74206861736800000000000000000000000000600082015250565b6000610c54601383610a9a565b9150610c5f82610c1e565b602082019050919050565b60006020820190508181036000830152610c8381610c47565b9050919050565b7f506172656e7420617373657420616c7265616479206d65726765640000000000600082015250565b6000610cc0601b83610a9a565b9150610ccb82610c8a565b602082019050919050565b60006020820190508181036000830152610cef81610cb3565b9050919050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610d30826107d5565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff821415610d6357610d62610cf6565b5b600182019050919050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052602160045260246000fd5b6000606082019050610db260008301866108f4565b610dbf60208301856108f4565b610dcc60408301846108f4565b949350505050565b7f5265636f7264206e6f7420666f756e6400000000000000000000000000000000600082015250565b6000610e0a601083610a9a565b9150610e1582610dd4565b602082019050919050565b60006020820190508181036000830152610e3981610dfd565b905091905056fea26469706673582212201de64bdc92c89a9f07e837eea93bb5b2c46a26b2879fb31a9e87fe39cc242c7864736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"bytes32\",\"name\":\"newAssetHash\",\"type\":\"bytes32\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"parentsCount\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"totalAmount\",\"type\":\"uint256\"},{\"indexed\":true,\"internalType\":\"address\",\"name\":\"operator\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"timestamp\",\"type\":\"uint256\"}],\"name\":\"BillMergeExecuted\",\"type\":\"event\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"bytes32[]\",\"name\":\"_pHashes\",\"type\":\"bytes32[]\"},{\"internalType\":\"bytes32\",\"name\":\"_newHash\",\"type\":\"bytes32\"},{\"internalType\":\"uint256\",\"name\":\"_amount\",\"type\":\"uint256\"},{\"internalType\":\"uint8\",\"name\":\"_type\",\"type\":\"uint8\"}],\"name\":\"executeMerge\",\"outputs\":[],\"selector\":[753126098,501762009],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":1,\"value\":[0]}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"_pHash\",\"type\":\"bytes32\"}],\"name\":\"getDestinationHash\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"}],\"selector\":[3487438119,3434986422],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":0,\"value\":[0]}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"_newHash\",\"type\":\"bytes32\"}],\"name\":\"getMergeDetail\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"time\",\"type\":\"uint256\"},{\"internalType\":\"address\",\"name\":\"op\",\"type\":\"address\"},{\"internalType\":\"bytes32[]\",\"name\":\"parents\",\"type\":\"bytes32[]\"}],\"selector\":[3067131015,1229297404],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":0,\"value\":[0]}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"_newHash\",\"type\":\"bytes32\"}],\"name\":\"getSourceHashes\",\"outputs\":[{\"internalType\":\"bytes32[]\",\"name\":\"\",\"type\":\"bytes32[]\"}],\"selector\":[3636746377,1419362900],\"stateMutability\":\"view\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_EXECUTEMERGE = "executeMerge";

    public static final String FUNC_GETDESTINATIONHASH = "getDestinationHash";

    public static final String FUNC_GETMERGEDETAIL = "getMergeDetail";

    public static final String FUNC_GETSOURCEHASHES = "getSourceHashes";

    public static final Event BILLMERGEEXECUTED_EVENT = new Event("BillMergeExecuted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    protected BillMergeV2(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public List<BillMergeExecutedEventResponse> getBillMergeExecutedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(BILLMERGEEXECUTED_EVENT, transactionReceipt);
        ArrayList<BillMergeExecutedEventResponse> responses = new ArrayList<BillMergeExecutedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BillMergeExecutedEventResponse typedResponse = new BillMergeExecutedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newAssetHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.operator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.parentsCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.totalAmount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeBillMergeExecutedEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(BILLMERGEEXECUTED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeBillMergeExecutedEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(BILLMERGEEXECUTED_EVENT);
        subscribeEvent(topic0,callback);
    }

    /**
     * 执行合并上链 
     * @param _amount 合并后的总金额 
     * @param _newHash 生成的新票据哈希 
     * @param _pHashes 待合并的父票据哈希数组 
     * @param _type 资产类型 (0-Bill, 1-Receipt) 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     */
    public TransactionReceipt executeMerge(List<byte[]> _pHashes, byte[] _newHash,
            BigInteger _amount, BigInteger _type) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_EXECUTEMERGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(_pHashes, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(_amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_type)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodExecuteMergeRawFunction(List<byte[]> _pHashes, byte[] _newHash,
            BigInteger _amount, BigInteger _type) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXECUTEMERGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(_pHashes, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(_amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_type)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForExecuteMerge(List<byte[]> _pHashes, byte[] _newHash,
            BigInteger _amount, BigInteger _type) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_EXECUTEMERGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(_pHashes, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(_amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_type)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 执行合并上链 
     * @param _amount 合并后的总金额 
     * @param _newHash 生成的新票据哈希 
     * @param _pHashes 待合并的父票据哈希数组 
     * @param _type 资产类型 (0-Bill, 1-Receipt) 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     * @return txHash Transaction hash of current transaction call 
     */
    public String executeMerge(List<byte[]> _pHashes, byte[] _newHash, BigInteger _amount,
            BigInteger _type, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_EXECUTEMERGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(_pHashes, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(_amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_type)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple4<List<byte[]>, byte[], BigInteger, BigInteger> getExecuteMergeInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXECUTEMERGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Bytes32>>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple4<List<byte[]>, byte[], BigInteger, BigInteger>(

                convertToNative((List<Bytes32>) results.get(0).getValue()), 
                (byte[]) results.get(1).getValue(), 
                (BigInteger) results.get(2).getValue(), 
                (BigInteger) results.get(3).getValue()
                );
    }

    public byte[] getDestinationHash(byte[] _pHash) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETDESTINATIONHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_pHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodGetDestinationHashRawFunction(byte[] _pHash) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETDESTINATIONHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_pHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return function;
    }

    public Tuple4<BigInteger, BigInteger, String, List<byte[]>> getMergeDetail(byte[] _newHash)
            throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETMERGEDETAIL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<DynamicArray<Bytes32>>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = executeCallWithMultipleValueReturn(function);
        return new Tuple4<BigInteger, BigInteger, String, List<byte[]>>(

                (BigInteger) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue(), 
                (String) results.get(2).getValue(), 
                convertToNative((List<Bytes32>) results.get(3).getValue())
                );
    }

    public Function getMethodGetMergeDetailRawFunction(byte[] _newHash) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETMERGEDETAIL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<DynamicArray<Bytes32>>() {}));
        return function;
    }

    @SuppressWarnings("rawtypes")
public List getSourceHashes(byte[] _newHash) throws ContractException {
        final Function function = new Function(FUNC_GETSOURCEHASHES, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Bytes32>>() {}));
        List<Type> result = (List<Type>) executeCallWithSingleValueReturn(function, List.class);
        return convertToNative(result);
    }

    public Function getMethodGetSourceHashesRawFunction(byte[] _newHash) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETSOURCEHASHES, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_newHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Bytes32>>() {}));
        return function;
    }

    public static BillMergeV2 load(String contractAddress, Client client,
            CryptoKeyPair credential) {
        return new BillMergeV2(contractAddress, client, credential);
    }

    public static BillMergeV2 deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        return deploy(BillMergeV2.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
    }

    public static class BillMergeExecutedEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] newAssetHash;

        public String operator;

        public BigInteger parentsCount;

        public BigInteger totalAmount;

        public BigInteger timestamp;
    }
}
