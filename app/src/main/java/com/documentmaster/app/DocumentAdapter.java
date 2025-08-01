package com.documentmaster.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<Document> documentList;
    private OnDocumentClickListener listener;

    public interface OnDocumentClickListener {
        void onDocumentClick(Document document);
        void onDocumentLongClick(Document document);
    }

    public DocumentAdapter(List<Document> documentList, OnDocumentClickListener listener) {
        this.documentList = documentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documentList.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageIcon;
        private TextView textName, textSize, textDate, textType;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.imageIcon);
            textName = itemView.findViewById(R.id.textName);
            textSize = itemView.findViewById(R.id.textSize);
            textDate = itemView.findViewById(R.id.textDate);
            textType = itemView.findViewById(R.id.textType);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDocumentClick(documentList.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDocumentLongClick(documentList.get(getAdapterPosition()));
                }
                return true;
            });
        }

        public void bind(Document document) {
            textName.setText(document.getName());
            textSize.setText(document.getFormattedSize());
            textDate.setText(document.getFormattedDate());
            textType.setText(document.getType());
            imageIcon.setImageResource(document.getTypeIcon());
        }
    }
}