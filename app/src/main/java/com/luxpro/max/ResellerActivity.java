package com.luxpro.max;

import android.os.Bundle;
import android.widget.ExpandableListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ResellerActivity extends BaseActivity {

    private ExpandableListView resellerListView;
    private ResellerAdapter listAdapter;
    private List<String> listDataHeader;
    private HashMap<String, List<ResellerModel>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resellers);

        resellerListView = findViewById(R.id.expandableListView);
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        fetchResellers();
    }

    public void fetchResellers() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("Resellers");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listDataHeader.clear();
                listDataChild.clear();

                for (DataSnapshot countrySnap : snapshot.getChildren()) {
                    String country = countrySnap.getKey();
                    listDataHeader.add(country);

                    List<ResellerModel> resellers = new ArrayList<>();
                    for (DataSnapshot resSnap : countrySnap.getChildren()) {
                        ResellerModel model = resSnap.getValue(ResellerModel.class);
                        if (model != null) {
                            resellers.add(model);
                        }
                    }
                    listDataChild.put(country, resellers);
                }

                // تحديث الـ Adapter
                listAdapter = new ResellerAdapter(ResellerActivity.this, listDataHeader, listDataChild);
                resellerListView.setAdapter(listAdapter);

                // فتح جميع المجموعات تلقائياً عند التحميل
                for (int i = 0; i < listAdapter.getGroupCount(); i++) {
                    resellerListView.expandGroup(i);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ResellerActivity.this, getString(R.string.error) + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}