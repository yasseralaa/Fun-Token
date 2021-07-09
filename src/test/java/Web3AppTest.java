import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.generated.contracts.FunToken;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;


public class Web3AppTest {

    private static FunToken funTokenOwner1, funTokenOwner2;
    private static Credentials account1Credentials, account2Credentials;
    private static final String account1Address = "ACCOUNT1 ADDRESS";
    private static final String account2Address = "ACCOUNT2 ADDRESS";
    private static final String account3Address = "ACCOUNT3 ADDRESS";
    private static Web3j web3j;
    private static final String nodeUrl = System.getenv().getOrDefault("WEB3J_NODE_URL", "NODE URL");

    @BeforeAll
    public static void setUp() throws Exception{
        account1Credentials = Credentials.create("ACC1 PK");
        account2Credentials = Credentials.create("ACC2 PK");

        web3j = Web3j.build(new HttpService(nodeUrl));

        funTokenOwner1 = FunToken.deploy(web3j, account1Credentials, new DefaultGasProvider()).send();
        funTokenOwner2 = FunToken.load(funTokenOwner1.getContractAddress(), web3j, account2Credentials, new DefaultGasProvider());
    }

    @BeforeEach
    @Test
    public void deployAndLoadContract() throws Exception{
        funTokenOwner1 = FunToken.deploy(web3j, account1Credentials, new DefaultGasProvider()).send();
        funTokenOwner2 = FunToken.load(funTokenOwner1.getContractAddress(), web3j, account2Credentials, new DefaultGasProvider());
    }


    @Test
    @DisplayName("total supply should be 100 initial")
    public void testTotalSupplyOnDeploy() throws Exception{
        BigInteger totalSupply = funTokenOwner1.totalSupply().send();
        assertEquals(new BigInteger("100"), totalSupply);
    }

    @Test
    @DisplayName("decimals should be 0")
    public void testDecimalsOnDeploy() throws Exception{
        BigInteger decimals = funTokenOwner1.decimals().send();
        assertEquals(new BigInteger("0"), decimals);
    }

    @Test
    @DisplayName("name should be Fun Token")
    public void testNameOnDeploy() throws Exception{
        String name = funTokenOwner1.name().send();
        assertEquals("Fun Token", name);
    }

    @Test
    @DisplayName("symbol should be FUN")
    public void testSymbolDeploy() throws Exception{
        String symbol = funTokenOwner1.symbol().send();
        assertEquals("FUN", symbol);
    }

    @Test
    @DisplayName("contract owner should have 100 FunToken")
    public void testContractOwnerBudget() throws Exception{
        BigInteger account1Bal = funTokenOwner1.balanceOf(account1Address).send();
        assertEquals(new BigInteger("100"), account1Bal);
    }

    @Test
    @DisplayName("other accounts should have 0 FunToken")
    public void testOtherAccountsBudget() throws Exception{
        BigInteger account2Bal = funTokenOwner2.balanceOf(account2Address).send();
        BigInteger account3Bal = funTokenOwner2.balanceOf(account3Address).send();
        assertEquals(new BigInteger("0"), account2Bal);
        assertEquals(new BigInteger("0"), account3Bal);
    }

    @Test
    @DisplayName("transfer 10 FUNs from account 1 to account 2")
    public void testTransferFromAccount1ToAccount2() throws Exception{
        TransactionReceipt transferTransactionReceipt = funTokenOwner1.transfer(account2Address, new BigInteger("10")).send();
        assertNotNull(transferTransactionReceipt.getTransactionHash());

        BigInteger account1Bal = funTokenOwner1.balanceOf(account1Address).send();
        assertEquals(new BigInteger("90"), account1Bal);

        BigInteger account2Bal = funTokenOwner1.balanceOf(account2Address).send();
        assertEquals(new BigInteger("10"), account2Bal);
    }

    @Test
    @DisplayName("account2 cannot transfer to/from other accounts without approval from contract deployer")
    public void testTransferWithoutApproval() throws Exception{

        assertThrows(Exception.class, () -> {
            TransactionReceipt transferTransactionReceipt = funTokenOwner2.transferFrom(account1Address, account3Address, new BigInteger("10")).send();
        });
        BigInteger account1Bal = funTokenOwner1.balanceOf(account1Address).send();
        assertEquals(new BigInteger("100"), account1Bal);

        BigInteger account2Bal = funTokenOwner1.balanceOf(account2Address).send();
        assertEquals(new BigInteger("0"), account2Bal);

        BigInteger account3Bal = funTokenOwner1.balanceOf(account3Address).send();
        assertEquals(new BigInteger("0"), account3Bal);
    }


    @Test
    @DisplayName("account2 cannot transfer to/from other accounts within approved limit")
    public void testTransferWithoutApprovedLimit() throws Exception{

        TransactionReceipt transferTransactionReceipt = funTokenOwner1.approve(account2Address, new BigInteger("10")).send();
        assertNotNull(transferTransactionReceipt.getTransactionHash());
        assertThrows(Exception.class, () -> {
            TransactionReceipt transferTransactionReceipt2 = funTokenOwner2.transferFrom(account1Address, account3Address, new BigInteger("15")).send();
        });
        BigInteger account1Bal = funTokenOwner1.balanceOf(account1Address).send();
        assertEquals(new BigInteger("100"), account1Bal);

        BigInteger account2Bal = funTokenOwner1.balanceOf(account2Address).send();
        assertEquals(new BigInteger("0"), account2Bal);

        BigInteger account3Bal = funTokenOwner1.balanceOf(account3Address).send();
        assertEquals(new BigInteger("0"), account3Bal);

        BigInteger allownce = funTokenOwner1.allowance(account1Address, account2Address).send();
        assertEquals(new BigInteger("10"), allownce);
    }

    @Test
    @DisplayName("account2 can transfer to/from other accounts within approved limit")
    public void testTransferWithApprovedLimit() throws Exception{

        TransactionReceipt transferTransactionReceipt = funTokenOwner1.approve(account2Address, new BigInteger("10")).send();
        assertNotNull(transferTransactionReceipt.getTransactionHash());

        TransactionReceipt transferTransactionReceipt2 = funTokenOwner2.transferFrom(account1Address, account3Address, new BigInteger("5")).send();
        assertNotNull(transferTransactionReceipt2.getTransactionHash());

        BigInteger account1Bal = funTokenOwner1.balanceOf(account1Address).send();
        assertEquals(new BigInteger("95"), account1Bal);

        BigInteger account2Bal = funTokenOwner1.balanceOf(account2Address).send();
        assertEquals(new BigInteger("0"), account2Bal);

        BigInteger account3Bal = funTokenOwner1.balanceOf(account3Address).send();
        assertEquals(new BigInteger("5"), account3Bal);

        BigInteger allownce = funTokenOwner1.allowance(account1Address, account2Address).send();
        assertEquals(new BigInteger("5"), allownce);

    }

}
