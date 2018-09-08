package net.cryptobrewery.link;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.codec.DecoderException;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static android.util.Base64.DEFAULT;
import static android.util.Base64.decode;
import static android.util.Base64.encodeToString;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHex;

public class MainActivity extends AppCompatActivity {
    byte [] encryptedBytes,decryptedBytes;
    PrivateKey privateKey;
    PublicKey publicKey;
    String decrypted;
    FirebaseDatabase db;
    DatabaseReference mDatabaseReference;
    String publicKeyString;
    private TextView MessageFlow;
    private Button senderBtn,ReceiverBtn;
    private EditText MessageTunnel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageFlow = findViewById(R.id.msgflow);
        senderBtn = findViewById(R.id.SenderBtn);
        ReceiverBtn =  findViewById(R.id.ReceiverBtn);
        MessageTunnel = findViewById(R.id.tunnel);

        senderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getChatKey();
                MessageFlow.setVisibility(View.GONE);
                senderBtn.setVisibility(View.GONE);
                ReceiverBtn.setVisibility(View.GONE);

                MessageTunnel.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        try {
                            uploadMessageToDatabase(new String(encodeHex(RSAEncrypt(editable.toString(),publicKey))));

                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        ReceiverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                senderBtn.setVisibility(View.GONE);
                ReceiverBtn.setVisibility(View.GONE);
                try {
                    // 1024-bit RSA key pair
                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(1024);
                    KeyPair keypair = keyGen.genKeyPair();
                    privateKey = keypair.getPrivate();
                    publicKey = keypair.getPublic();
                    uploadPublicKeyToDatabase(publicKey);


                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                keepFlowUpdated();
            }
        });
        db = FirebaseDatabase.getInstance();
        mDatabaseReference = db.getReference().child("Chat");



    }

    private void keepFlowUpdated() {
        MessageTunnel.setVisibility(View.GONE);
        mDatabaseReference.child("flow").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if(dataSnapshot.getValue()!=null){
                        MessageFlow.setText(RSADecrypt(hexStringToByteArray(dataSnapshot.getValue(String.class)),privateKey));
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadMessageToDatabase(String s) {
        mDatabaseReference.child("flow").setValue(s).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, "Message Uploaded", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public byte[] RSAEncrypt(final String plain,PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {



        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        encryptedBytes = cipher.doFinal(plain.getBytes());
        Log.d("encrypt encrypted", new String(encodeHex(encryptedBytes)));
        return encryptedBytes;
    }
    public String RSADecrypt(byte[] encryptedBytes,PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher1 = Cipher.getInstance("RSA");
        cipher1.init(Cipher.DECRYPT_MODE, privateKey);
        decryptedBytes = cipher1.doFinal(encryptedBytes);
        decrypted = new String(decryptedBytes);
        Log.d("encrypt decrypted",decrypted);
        return decrypted;
    }
    public void uploadPublicKeyToDatabase(PublicKey key){
        String keyAsString = publicKeyToString(key);
        mDatabaseReference.child("PublicKey").setValue(keyAsString).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, "Key Uploaded To Database", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public  String publicKeyToString(PublicKey publ) {
        String publicKeyString = null;
        try {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec spec = fact.getKeySpec(publ,
                    X509EncodedKeySpec.class);
            publicKeyString = encodeToString(spec.getEncoded(), DEFAULT);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return publicKeyString;
    }
    public  PublicKey stringToPublicKey(String publStr) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = decode(publStr.getBytes("utf-8"),0);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Toast.makeText(this, "key is ready", Toast.LENGTH_SHORT).show();
        return  keyFactory.generatePublic(spec);
    }

    public void getChatKey(){
        mDatabaseReference.child("PublicKey").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Toast.makeText(MainActivity.this, "Chat Key is downloaded", Toast.LENGTH_SHORT).show();
                    publicKey = stringToPublicKey(dataSnapshot.getValue(String.class));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    public static byte[] hexStringToByteArray(String hex) {
        int l = hex.length();
        byte[] data = new byte[l/2];
        for (int i = 0; i < l; i += 2) {
            data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
