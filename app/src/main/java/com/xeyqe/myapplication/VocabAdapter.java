package com.xeyqe.myapplication;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class VocabAdapter extends RecyclerView.Adapter<VocabAdapter.VocabHolder> {
    private List<Vocab> vocabs = new ArrayList<>();
    private OnItemClickListener listener;

    @NonNull
    @Override
    public VocabHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.vocab_item, parent, false);
        return new VocabHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull VocabHolder holder, int position) {
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

    public Vocab getVocabAt(int position) {
        return vocabs.get(position);
    }

    class VocabHolder extends RecyclerView.ViewHolder {
        private TextView textViewWord;
        private TextView textViewMeaning;
        private TextView textViewLanguage;

        public VocabHolder(View itemView) {
            super(itemView);
            textViewWord = itemView.findViewById(R.id.text_view_title);
            textViewMeaning = itemView.findViewById(R.id.text_view_description);
            textViewLanguage = itemView.findViewById(R.id.text_view_priority);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null) {
                        listener.onItemClick(vocabs.get(position));
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Vocab vocab);
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}