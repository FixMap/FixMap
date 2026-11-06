package com.fixmap.controller.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.StringJoiner;

import com.fixmap.dao.ReportDao;
import com.fixmap.dto.Report;
import com.fixmap.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** GET /api/reports/map?floor={층 id} : 지도에 그릴 신고 핀 */
@WebServlet("/api/reports/map")
public class ReportMapApiController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final transient ReportDao reportDao = new ReportDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long floorId;
        try {
            floorId = Long.parseLong(req.getParameter("floor"));
        } catch (NumberFormatException e) {
            JsonUtil.write(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"message\":\"floor 파라미터가 필요합니다\"}");
            return;
        }
        try {
            StringJoiner arr = new StringJoiner(",", "[", "]");
            for (Report r : reportDao.findPinsByFloor(floorId)) {
                arr.add("{\"id\":" + r.getId()
                        + ",\"title\":" + JsonUtil.quote(r.getTitle())
                        + ",\"posX\":" + r.getPosX()
                        + ",\"posY\":" + r.getPosY()
                        + ",\"status\":" + JsonUtil.quote(r.getStatus()) + "}");
            }
            JsonUtil.write(resp, HttpServletResponse.SC_OK, arr.toString());
        } catch (SQLException e) {
            log("신고 핀 조회 실패", e);
            JsonUtil.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"message\":\"DB 오류\"}");
        }
    }
}
