package com.xeyqe.myapplication;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class VocabAdapter extends RecyclerView.Adapter<VocabAdapter.NoteHolder> {
    private List<Vocab> vocabs = new ArrayList<>();

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.vocab_item, parent, false);
        return new NoteHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHolder holder, int position) {
        Vocab currentVocab = vocabs.get(position);
        holder.textViewWord.setText(currentVocab.getWord());
        holder.textViewMeaning.setText(Html.fromHtml(currentVocab.getMeaning()));
        holder.textViewLanguage.setText(currentVocab.getLanguage());
    }

    @Override
    public int getItemCount() {
        return vocabs.size();
    }

    public void setVocabs(List<Vocab> vocabs) {
        this.vocabs = vocabs;
        notifyDataSetChanged();
    }

    class NoteHolder extends RecyclerView.ViewHolder {
        private TextView textViewWord;
        private TextView textViewMeaning;
        private TextView textViewLanguage;

        public NoteHolder(View itemView) {
            super(itemView);
            textViewWord = itemView.findViewById(R.id.text_view_title);
            textViewMeaning = itemView.findViewById(R.id.text_view_description);
            textViewLanguage = itemView.findViewById(R.id.text_view_priority);
        }
    }
}