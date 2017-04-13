package com.example.thinkmobiles.bitcoinwalletsample.main;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.example.thinkmobiles.bitcoinwalletsample.Constants;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.util.Date;

/**
 * Created by Lynx on 4/11/2017.
 */

public class MainActivityPresenter implements MainActivityContract.MainActivityPresenter {

    private MainActivityContract.MainActivityView view;
    private File walletDir; //Context.getCacheDir();

    private NetworkParameters parameters;
    private WalletAppKit walletAppKit;

    public MainActivityPresenter(MainActivityContract.MainActivityView view, File walletDir) {
        this.view = view;
        this.walletDir = walletDir;

        view.setPresenter(this);
    }

    @Override
    public void subscribe() {
        setBtcSDKThread();
        parameters = Constants.IS_PRODUCTION ? MainNetParams.get() : TestNet3Params.get();
        BriefLogFormatter.init();

        walletAppKit = new WalletAppKit(parameters, walletDir, Constants.WALLET_NAME) {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getImportedKeys().size() < 1) wallet().importKey(new ECKey());
                wallet().allowSpendingUnconfirmedTransactions();
                view.displayWalletPath(vWalletFile.getAbsolutePath());
                setupWalletListeners(wallet());

                Log.d("myLogs", "My address = " + wallet().freshReceiveAddress());
            }
        };
        walletAppKit.setDownloadListener(new DownloadProgressTracker() {
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                int percentage = (int) pct;
                view.displayPercentage(percentage);
                view.displayProgress(percentage);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                view.displayDownloadContent(false);
                refresh();
            }
        });
        walletAppKit.setBlockingStartup(false);
        walletAppKit.startAsync();
    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void refresh() {
        String myAddress = walletAppKit.wallet().freshReceiveAddress().toBase58();

        view.displayMyBalance(walletAppKit.wallet().getBalance().toFriendlyString());
        view.displayMyAddress(myAddress);

    }

    @Override
    public void pickRecipient() {
        view.displayRecipientAddress(null);
        view.startScanQR();
    }

    @Override
    public void send() {
        String recipientAddress = view.getRecipient();
        String amount = view.getAmount();
        if(TextUtils.isEmpty(recipientAddress) || recipientAddress.equals("Scan recipient QR")) {
            view.showToastMessage("Select recipient");
            return;
        }
        if(TextUtils.isEmpty(amount) | Double.parseDouble(amount) <= 0) {
            view.showToastMessage("Select valid amount");
            return;
        }
        if(walletAppKit.wallet().getBalance().isLessThan(Coin.parseCoin(amount))) {
            view.showToastMessage("You got not enough coins");
            view.clearAmount();
            return;
        }
        SendRequest request = SendRequest.to(Address.fromBase58(parameters, recipientAddress), Coin.parseCoin(amount));
        try {
            walletAppKit.wallet().completeTx(request);
            walletAppKit.wallet().commitTx(request.tx);
            walletAppKit.peerGroup().broadcastTransaction(request.tx).broadcast();
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
            view.showToastMessage(e.getMessage());
        }

    }

    @Override
    public void getInfoDialog() {
        view.displayInfoDialog(walletAppKit.wallet().currentReceiveAddress().toBase58());
    }

    private void setBtcSDKThread() {
        final Handler handler = new Handler();
        Threading.USER_THREAD = handler::post;
    }

    private void setupWalletListeners(Wallet wallet) {
        wallet.addCoinsReceivedEventListener((wallet1, tx, prevBalance, newBalance) -> {
            view.displayMyBalance(wallet.getBalance().toFriendlyString());
            if(tx.getPurpose() == Transaction.Purpose.UNKNOWN)
            view.showToastMessage("Receive " + newBalance.minus(prevBalance).toFriendlyString());
        });
        wallet.addCoinsSentEventListener((wallet12, tx, prevBalance, newBalance) -> {
            view.displayMyBalance(wallet.getBalance().toFriendlyString());
            view.clearAmount();
            view.displayRecipientAddress(null);
            view.showToastMessage("Sent " + prevBalance.minus(newBalance).minus(tx.getFee()).toFriendlyString());
        });
    }
}
