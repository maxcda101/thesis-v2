package jobs;

import entities.DataEmbed;
import entities.Response;
import models.Data;
import models.Node;
import models.Sensor;
import models.TypeData;
import org.joda.time.MutableDateTime;
import play.Logger;
import play.jobs.Job;

import java.util.*;

/**
 * Created by AnhQuan on 23/10/2016.
 */
public class UpdateData extends Job {
    private List<DataEmbed> listDataEmbed;

    public UpdateData(List<DataEmbed> listDataEmbed) {
        this.listDataEmbed = listDataEmbed;
    }

    @Override
    public void doJob() {
        Logger.info("Start job update data");
        List<DataEmbed> listAir = new ArrayList<DataEmbed>();
        List<DataEmbed> listTemp = new ArrayList<DataEmbed>();
        List<DataEmbed> listHumi = new ArrayList<DataEmbed>();

        Logger.info("size list bembed: " + listDataEmbed.size());
        for (DataEmbed dataEmbed : listDataEmbed) {
            if (dataEmbed.idSensor == 1l) {
                listAir.add(dataEmbed);
            } else if (dataEmbed.idSensor == 2l) {
                listTemp.add(dataEmbed);
            } else if (dataEmbed.idSensor == 3l) {
                listHumi.add(dataEmbed);
            }
        }
        if (listAir.size() > 0) {
            Logger.info("airrrrrrrrrrrrrrrrrrrrrrrrrrrr: " + listAir.size());
            handerDataEmbed(listAir);
        }
        if (listTemp.size() > 0) {
            Logger.info("temppppppppppppppppppppppppppp: " + listTemp.size());
            handerDataEmbed(listTemp);
        }
        if (listHumi.size() > 0) {
            Logger.info("humiiiiiiiiiiiiiiiiiiiiiiiiiiii: " + listHumi.size());
            handerDataEmbed(listHumi);
        }
    }

    private void handerDataEmbed(List<DataEmbed> list) {
        Collections.sort(list, new Comparator<DataEmbed>() {
            @Override
            public int compare(DataEmbed o1, DataEmbed o2) {
                if (o1.idNode > o2.idNode) {
                    return -1;
                }
                if (o1.idNode < o2.idNode) {
                    return 1;
                }
                return 0;
            }
        });
        long idNode = list.get(0).idNode;
        List<DataEmbed> affterHander = new ArrayList<DataEmbed>();
        for (int i = 0; i < list.size(); i++) {
            DataEmbed dataEmbed = list.get(i);
            if (dataEmbed.idNode != idNode || i == list.size() - 1) {
                if (i == list.size() - 1) {
                    affterHander.add(dataEmbed);
                }
                Logger.info("node---------- " + idNode + "-----------");
                Logger.info("Size: " + affterHander.size());
                if (affterHander.size() > 0) {
                    synchonizeData(affterHander);
                }
                affterHander.removeAll(affterHander);
                idNode = dataEmbed.idNode;
            }
            affterHander.add(dataEmbed);
        }

    }

    private void synchonizeData(List<DataEmbed> dataList) {
        Data data = dataList.get(0).convertData();
        Node node = data.node;
        Sensor sensor = data.sensor;
        MutableDateTime temp = new MutableDateTime(data.timeCreate);
        MutableDateTime temp2;

        TypeData mediumType = TypeData.find("byName", "medium 1h").first();
        TypeData maxType = TypeData.find("byName", "max 1h").first();
        TypeData minType = TypeData.find("byName", "min 1h").first();
        TypeData nowType = TypeData.find("byName", "now").first();

        for (int i = 0; i < dataList.size(); i++) {
            DataEmbed dataEmbed = dataList.get(i);
            try {
                data = dataEmbed.convertData();
                if (data.value > 0f) {
                    data.save();
                }
                temp2 = new MutableDateTime(data.timeCreate != null ? data.timeCreate : new Date());
                if (temp.getDayOfYear() != temp2.getDayOfYear() || temp.getHourOfDay() != temp2.getHourOfDay() || i == dataList.size() - 1) {
                    Logger.info("Update data in: " + temp.getDayOfMonth() + "-" + temp.getHourOfDay());

                    Data dataMedium = Data.find("year(timeCreate)=? AND dayofyear(timeCreate)=? AND hour(timeCreate)=? AND typeData=? AND sensor=? AND node=?",temp.getYear(), temp.getDayOfYear(), temp.getHourOfDay(), mediumType, sensor, node).first();
                    Data dataMax = Data.find("year(timeCreate)=? AND dayofyear(timeCreate)=? AND hour(timeCreate)=? AND typeData=? AND sensor=? AND node=?",temp.getYear(), temp.getDayOfYear(), temp.getHourOfDay(), maxType, sensor, node).first();
                    Data dataMin = Data.find("year(timeCreate)=? AND dayofyear(timeCreate)=? AND hour(timeCreate)=? AND typeData=? AND sensor=? AND node=?",temp.getYear(), temp.getDayOfYear(), temp.getHourOfDay(), minType, sensor, node).first();
                    List<Data> list = Data.find("year(timeCreate)=? AND dayofyear(timeCreate)=? AND hour(timeCreate)=? AND typeData=? AND sensor=? AND node=?",temp.getYear(), temp.getDayOfYear(), temp.getHourOfDay(), nowType, sensor, node).fetch();

                    temp.setMinuteOfHour(0);
                    float value=list.get(0).value;
                    if (dataMax == null) {
                        dataMax=new Data();
                        dataMax.node=node;
                        dataMax.sensor=sensor;
                        dataMax.typeData = maxType;
                        dataMax.value=value;
                        dataMax.timeCreate = temp.toDate();
                    }
                    if (dataMedium == null) {
                        dataMedium=new Data();
                        dataMedium.node=node;
                        dataMedium.sensor=sensor;
                        dataMedium.typeData = mediumType;
                        dataMedium.value=value;
                        dataMedium.timeCreate = temp.toDate();
                    }
                    if (dataMin == null) {
                        dataMin=new Data();
                        dataMin.node=node;
                        dataMin.sensor=sensor;
                        dataMin.typeData = minType;
                        dataMin.value=value;
                        dataMin.timeCreate = temp.toDate();
                    }

                    float tong = 0;
                    int index = 0;

                    for (Data data1 : list) {
                        if (data1.value < dataMin.value) {
                            float v=data1.value;
                            dataMin.value = v;
                        }
                        if (data1.value > dataMax.value) {
                            float v=data1.value;
                            dataMax.value =v;
                        }
                        index++;
                        tong += data1.value;
                    }
                    dataMedium.value = tong / index;

                    dataMin.timeReceived = new Date();
                    dataMax.timeReceived = new Date();
                    dataMedium.timeReceived = new Date();
                    dataMin.save();
                    dataMax.save();
                    dataMedium.save();

                    Logger.info(String.format("MAX: type=%s | Node=%s | Value=%s | Sensor=%s | Time=%s", dataMax.typeData.name, dataMax.node.name, dataMax.value, dataMax.sensor.name, dataMax.timeCreate));
                    Logger.info(String.format("MIN:type=%s | Node=%s | Value=%s | Sensor=%s | Time=%s", dataMin.typeData.name, dataMin.node.name, dataMin.value, dataMin.sensor.name, dataMin.timeCreate));
                    Logger.info(String.format("MEDIUM:type=%s | Node=%s | Value=%s | Sensor=%s | Time=%s", dataMedium.typeData.name, dataMedium.node.name, dataMedium.value, dataMedium.sensor.name, dataMedium.timeCreate));
                    temp = temp2;

                    dataMin=null;
                    dataMax=null;
                    dataMedium=null;
                } else {
                }
            } catch (Exception e) {
                Logger.error(e, "error");
            }
        }
    }
}
