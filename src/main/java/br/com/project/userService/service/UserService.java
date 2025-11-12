package br.com.project.userService.service;

import java.util.List;

import org.springframework.stereotype.Service;

// REMOVA a importação do PasswordEncoder
// import org.springframework.security.crypto.password.PasswordEncoder; 
// REMOVA a importação do Mapper (Dozer)
// import com.github.dozermapper.core.Mapper; 

import br.com.project.userService.domain.UserEntity;
import br.com.project.userService.dto.UserDTO;
import br.com.project.userService.exception.RecordNotFoundException;
import br.com.project.userService.repository.UserRepository;
import br.com.project.userService.factory.UserFactory; // <-- ADICIONE a importação da Factory
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserFactory userFactory; // <-- SUBSTITUA o Mapper e o PasswordEncoder pela Factory

    // REMOVA as dependências antigas:
    // private final Mapper mapper;
    // private final PasswordEncoder passwordEncoder;

    public UserDTO create(UserDTO dto) {
        // Use a Factory para encapsular a criação e o hash da senha
        UserEntity entity = userFactory.createEntityFromDTO(dto);

        // O código antigo foi substituído:
        // UserEntity entity = mapper.map(dto, UserEntity.class);
        // entity.setId(null); 
        // entity.setPassword(passwordEncoder.encode(dto.getPassword()));

        UserEntity result = repository.save(entity);
        
        // Use a Factory para criar o DTO de resposta
        return userFactory.createDTOFromEntity(result);
    }

    public UserDTO update(long id, UserDTO source) {
        UserEntity target = repository.findById(id).orElseThrow(RecordNotFoundException::new);
        target.setUsername(source.getUsername());
        target.setRoles(source.getRoles());
        // (Seu código original não atualizava a senha aqui, então mantemos isso)
        
        UserEntity result = repository.save(target);
        
        // Use a Factory para criar o DTO de resposta
        return userFactory.createDTOFromEntity(result);
    }

    public void delete(long id) {
        UserEntity entity = repository.findById(id).orElseThrow(RecordNotFoundException::new);
        repository.delete(entity);
    }

    public UserDTO findById(long id) {
        UserEntity result = repository.findById(id).orElseThrow(RecordNotFoundException::new);
        
        // Use a Factory para criar o DTO de resposta
        return userFactory.createDTOFromEntity(result);
    }

    public Iterable<UserDTO> findAll() {
        List<UserEntity> entities = repository.findAll();
        List<UserDTO> dtos = entities.stream()
                // Use a Factory (com method reference) para o "map"
                .map(userFactory::createDTOFromEntity) 
                .toList();
        return dtos;
    }
}