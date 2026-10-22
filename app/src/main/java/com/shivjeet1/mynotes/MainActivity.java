package com.shivjeet1.mynotes;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationBarView;
import com.shivjeet1.mynotes.util.WindowInsetsHelper;

/**
 * Thin shell: one app bar per tab, one navigation bar (bottom on phones, rail
 * on tablets), and a single content container. Each tab owns its FAB so it
 * always clears the navigation bar.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_NOTES = "notes";
    private static final String TAG_TASKS = "tasks";
    private static final String STATE_TAB = "state_tab";

    private int currentTab = R.id.nav_notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowInsetsHelper.apply(this, findViewById(R.id.root));

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_notes);
        }

        NavigationBarView bottomNav = findViewById(R.id.bottomNav);
        // The shell root already pads for the navigation bar; stop the bar from
        // adding that inset a second time.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> insets);
        bottomNav.setSelectedItemId(currentTab);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        showTab(currentTab);
    }

    private void showTab(int itemId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment notes = getSupportFragmentManager().findFragmentByTag(TAG_NOTES);
        Fragment tasks = getSupportFragmentManager().findFragmentByTag(TAG_TASKS);

        Fragment target;
        if (itemId == R.id.nav_tasks) {
            if (tasks == null) {
                tasks = new TasksFragment();
                transaction.add(R.id.fragmentContainer, tasks, TAG_TASKS);
            }
            target = tasks;
        } else {
            if (notes == null) {
                notes = new NotesFragment();
                transaction.add(R.id.fragmentContainer, notes, TAG_NOTES);
            }
            target = notes;
        }

        if (notes != null && notes != target) {
            transaction.hide(notes);
        }
        if (tasks != null && tasks != target) {
            transaction.hide(tasks);
        }
        transaction.show(target);
        transaction.commit();
        currentTab = itemId;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }
}
