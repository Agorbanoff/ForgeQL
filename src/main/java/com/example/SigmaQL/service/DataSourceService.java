package com.example.SigmaQL.service;


import com.example.SigmaQL.common.exceptions.DataSourceAlreadyExistsException;
import com.example.SigmaQL.common.exceptions.NoDataSourceFoundException;
import com.example.SigmaQL.common.exceptions.UserNotFoundException;
import com.example.SigmaQL.controller.dtos.request.ReqDataSourceDTO;
import com.example.SigmaQL.controller.dtos.response.ResDataSourceDTO;
import com.example.SigmaQL.persistence.Enums.DataSourceStatus;
import com.example.SigmaQL.persistence.model.DataSourceEntity;
import com.example.SigmaQL.persistence.model.UserAccountEntity;
import com.example.SigmaQL.persistence.repository.DataSourceRepository;
import com.example.SigmaQL.persistence.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataSourceService {
    private final DataSourceRepository dataSourceRepository;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    public DataSourceService(DataSourceRepository dataSourceRepository,
                             UserAccountRepository userAccountRepository) {
        this.dataSourceRepository = dataSourceRepository;
        this.userAccountRepository = userAccountRepository;
    }

    private ResDataSourceDTO mapToDTO(DataSourceEntity dataSourceEntity) {
        ResDataSourceDTO resDataSourceDTO = new ResDataSourceDTO();
        resDataSourceDTO.setId(dataSourceEntity.getId());
        resDataSourceDTO.setName(dataSourceEntity.getName());
        resDataSourceDTO.setDbType(dataSourceEntity.getDbType());
        resDataSourceDTO.setHost(dataSourceEntity.getHost());
        resDataSourceDTO.setPort(dataSourceEntity.getPort());
        resDataSourceDTO.setDatabaseName(dataSourceEntity.getDatabaseName());
        resDataSourceDTO.setUsername(dataSourceEntity.getUsername());
        resDataSourceDTO.setSchemaName(dataSourceEntity.getSchemaName());
        resDataSourceDTO.setSslEnabled(dataSourceEntity.isSslEnabled());
        resDataSourceDTO.setSslMode(dataSourceEntity.getSslMode());

        return resDataSourceDTO;
    }

    public ResDataSourceDTO getDataSource(Integer id, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(id, userId)
                            .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        return mapToDTO(dataSourceEntity);
    }

    public List<ResDataSourceDTO> getAllDataSource(Integer userId) {
        List<DataSourceEntity> dataSourceEntities = dataSourceRepository.findAllByUserAccount_Id(userId);

        List<ResDataSourceDTO> resDataSourceDTOList = new ArrayList<>();
        for (DataSourceEntity dataSourceEntity : dataSourceEntities) {
            resDataSourceDTOList.add(mapToDTO(dataSourceEntity));
        }

        return resDataSourceDTOList;
    }

    public void saveDataSource(ReqDataSourceDTO reqDataSourceDTO, Integer userId) {
        UserAccountEntity userAccountEntity = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (dataSourceRepository.existsByUserAccountIdAndDbTypeAndHostAndPortAndDatabaseNameAndUsername(
                userId,
                reqDataSourceDTO.getDbType(),
                reqDataSourceDTO.getHost(),
                reqDataSourceDTO.getPort(),
                reqDataSourceDTO.getDatabaseName(),
                reqDataSourceDTO.getUsername()
        )) {
            throw new DataSourceAlreadyExistsException("Datasource already exists");
        }

        DataSourceEntity dataSourceEntity = new DataSourceEntity();
        dataSourceEntity.setName(reqDataSourceDTO.getName());
        dataSourceEntity.setDbType(reqDataSourceDTO.getDbType());
        dataSourceEntity.setHost(reqDataSourceDTO.getHost());
        dataSourceEntity.setPort(reqDataSourceDTO.getPort());
        dataSourceEntity.setDatabaseName(reqDataSourceDTO.getDatabaseName());
        dataSourceEntity.setSchemaName(reqDataSourceDTO.getSchemaName());
        dataSourceEntity.setUsername(reqDataSourceDTO.getUsername());
        dataSourceEntity.setEncryptedPassword(reqDataSourceDTO.getEncryptedPassword());
        dataSourceEntity.setSslEnabled(reqDataSourceDTO.getSslEnabled());

        if (Boolean.FALSE.equals(reqDataSourceDTO.getSslEnabled())) {
            dataSourceEntity.setSslMode(null);
        } else {
            dataSourceEntity.setSslMode(reqDataSourceDTO.getSslMode());
        }

        dataSourceEntity.setStatus(DataSourceStatus.ACTIVE);
        dataSourceEntity.setUserAccount(userAccountEntity);

        dataSourceRepository.save(dataSourceEntity);
    }

    public void deleteDataSource(Integer id, Integer userId) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(id, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        dataSourceRepository.delete(dataSourceEntity);
    }

    public void updateDataSource(ReqDataSourceDTO reqDataSourceDTO, Integer userId, Integer id) {
        DataSourceEntity dataSourceEntity = dataSourceRepository.findByIdAndUserAccount_Id(id, userId)
                .orElseThrow(() -> new NoDataSourceFoundException("Datasource not found"));

        if (dataSourceRepository.existsByUserAccountIdAndDbTypeAndHostAndPortAndDatabaseNameAndUsernameAndIdNot(
                userId,
                reqDataSourceDTO.getDbType(),
                reqDataSourceDTO.getHost(),
                reqDataSourceDTO.getPort(),
                reqDataSourceDTO.getDatabaseName(),
                reqDataSourceDTO.getUsername(),
                id
        )) {
            throw new DataSourceAlreadyExistsException("Datasource already exists");
        }

        dataSourceEntity.setName(reqDataSourceDTO.getName());
        dataSourceEntity.setDbType(reqDataSourceDTO.getDbType());
        dataSourceEntity.setHost(reqDataSourceDTO.getHost());
        dataSourceEntity.setPort(reqDataSourceDTO.getPort());
        dataSourceEntity.setDatabaseName(reqDataSourceDTO.getDatabaseName());
        dataSourceEntity.setUsername(reqDataSourceDTO.getUsername());
        dataSourceEntity.setEncryptedPassword(reqDataSourceDTO.getEncryptedPassword());
        dataSourceEntity.setSchemaName(reqDataSourceDTO.getSchemaName());
        dataSourceEntity.setSslEnabled(reqDataSourceDTO.getSslEnabled());

        if (Boolean.FALSE.equals(reqDataSourceDTO.getSslEnabled())) {
            dataSourceEntity.setSslMode(null);
        } else {
            dataSourceEntity.setSslMode(reqDataSourceDTO.getSslMode());
        }

        dataSourceRepository.save(dataSourceEntity);
    }
}
