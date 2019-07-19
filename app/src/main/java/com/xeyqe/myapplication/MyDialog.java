package com.xeyqe.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class MyDialog extends AppCompatDialogFragment {
    private EditText languages;
    private MyDialogListener listener;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog, null);

        builder.setView(view)
                .setTitle("Languages")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String language = languages.getText().toString();

                        listener.applyTexts(language);
                        listener.performFileSearch();
                        dialog.dismiss();

                    }
                });

        languages = view.findViewById(R.id.edit_view_lang);
        return builder.create();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (MyDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+"Must implement MyDialogListener");
        }
    }

    public interface MyDialogListener {
        void applyTexts(String lng);
        void performFileSearch();
    }
}
