package com.example.tiktok.entity;

import com.example.tiktok.config.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LabelDAO {

    public void insertLabel(String name, List<Double> embedding) throws Exception {
        String sql = "INSERT INTO label (name, vector) VALUES (?, ?::vector)";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setObject(2, convertListToArray(embedding));  // 转换 List<Double> 为 double[]
            pstmt.executeUpdate();
        }
    }

    public List<String> findSimilarLabels(List<Double> embedding) throws Exception {
        String sql = "SELECT name FROM label ORDER BY vector <-> ?::vector LIMIT 20";
        List<String> similarLabels = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, convertListToArray(embedding));  // 转换 List<Double> 为 double[]
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                similarLabels.add(rs.getString("name"));
            }
        }

        return similarLabels;
    }


    public List<String> findSimilarLabelsBatch(List<List<Double>> embeddings) throws Exception {
        if (embeddings.size() == 1) {
            return findSimilarLabels(embeddings.get(0));
        }
        StringBuilder sql = new StringBuilder("(SELECT name FROM label ORDER BY vector <-> ?::vector)");

        // 动态构建SQL查询，使用 UNION ALL 将多个向量查询合并
        for (int i = 1; i < embeddings.size(); i++) {
            sql.append(" UNION ALL (SELECT name FROM label ORDER BY vector <-> ?::vector)");
        }

        // 对合并后的结果进行排序并限制返回行数
        sql.append(" ORDER BY 1 LIMIT 5");

        List<String> similarLabels = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            // 将每个向量参数设置到 PreparedStatement 中
            for (int i = 0; i < embeddings.size(); i++) {
                pstmt.setObject(i + 1, convertListToArray(embeddings.get(i)));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                similarLabels.add(rs.getString("name"));
            }
        }
        return similarLabels;
    }

    public double[] convertListToArray(List<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

}

