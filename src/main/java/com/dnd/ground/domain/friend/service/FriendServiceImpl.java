package com.dnd.ground.domain.friend.service;

import com.dnd.ground.domain.friend.Friend;
import com.dnd.ground.domain.friend.FriendStatus;
import com.dnd.ground.domain.friend.dto.FriendResponseDto;
import com.dnd.ground.domain.friend.repository.FriendRepository;
import com.dnd.ground.domain.user.User;
import com.dnd.ground.domain.user.repository.UserRepository;
import com.dnd.ground.global.exception.ExceptionCodeSet;
import com.dnd.ground.global.exception.FriendException;
import com.dnd.ground.global.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author 박찬호
 * @description 친구와 관련된 서비스 구현체
 * @since 2022-08-01
 * @updated 1.페이징 수정
 *          - 2022.11.27 박찬호
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    private final Integer FRIEND_LARGE_PAGING_NUMBER = 15; //15개씩 페이징하기 위해 1개 더 가져옴(마지막 여부 판단)
    private final Integer FRIEND_SMALL_PAGING_NUMBER = 3; //3개씩 페이징 하기 위해 1개 더 가져옴.

    //친구 목록과 추가 정보를 함께 반환
    @Transactional
    public FriendResponseDto getFriends(String nickname, Integer offset) {

        //유저 및 친구 조회
        User user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        List<FriendResponseDto.FInfo> infos = new ArrayList<>();
        boolean isLast;

        PageRequest pageRequest = PageRequest.of(offset, FRIEND_LARGE_PAGING_NUMBER);

        try {
            Slice<Friend> findFriendSlice = friendRepository.findFriendsByUserWithPaging(user, user, pageRequest);
            isLast = findFriendSlice.isLast();

            List<Friend> findFriends = findFriendSlice.getContent();

            for (Friend findFriend : findFriends) {
                if (findFriend.getUser() == user) {
                    infos.add(FriendResponseDto.FInfo.of()
                            .nickname(findFriend.getFriend().getNickname())
                            .picturePath(findFriend.getFriend().getPicturePath())
                            .build());
                } else if (findFriend.getFriend() == user) {
                    infos.add(FriendResponseDto.FInfo.of()
                            .nickname(findFriend.getUser().getNickname())
                            .picturePath(findFriend.getFriend().getPicturePath())
                            .build());
                }
            }
            return FriendResponseDto.builder()
                    .infos(infos)
                    .size(findFriends.size())
                    .isLast(isLast)
                    .build();

        } catch (NullPointerException e) {
            return FriendResponseDto.builder()
                    .infos(infos)
                    .size(0)
                    .isLast(true)
                    .build();
        }
    }

    //요청받은 친구 목록 조회
    public FriendResponseDto.ReceiveRequest getReceiveRequest(String nickname, Integer offset) {
        User user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND));

        FriendResponseDto.ReceiveRequest response = new FriendResponseDto.ReceiveRequest();
        boolean isLast;

        PageRequest pageRequest = PageRequest.of(offset, FRIEND_SMALL_PAGING_NUMBER);
        Slice<User> receiveRequestSlice = friendRepository.findReceiveRequest(user, pageRequest);
        isLast = receiveRequestSlice.isLast();

        List<User> receiveRequest = receiveRequestSlice.getContent();

        for (User friend : receiveRequest) {
            response.getFriendsInfo().add(
                    FriendResponseDto.FInfo.of()
                            .nickname(friend.getNickname())
                            .picturePath(friend.getPicturePath())
                            .build()
            );
        }

        response.setSize(receiveRequest.size());
        response.setIsLast(isLast);
        return response;
    }

    //List<User> 형태의 친구 목록 반환
    public List<User> getFriends(User user) {
        List<Friend> findFriends = friendRepository.findFriendsByUser(user);
        List<User> friends = new ArrayList<>();

        for (Friend findFriend : findFriends) {
            if (findFriend.getUser() == user) {
                friends.add(findFriend.getFriend());
            } else if (findFriend.getFriend() == user) {
                friends.add(findFriend.getUser());
            }
        }

        return friends;
    }

    /**
     * 단건 친구 요청 *
     * 누가 요청을 보냈는지 알기 위해 다음과 같은 명확한 기준을 세운다.
     * 추후 친구 요청 수락을 위해 요청을 보내는 쪽이 user, 받는 쪽이 friend에 저장한다.
     * 단, 한 친구 관계는 DB에 1번만 저장하도록 한다.
     *
     * @param userNickname
     * @param friendNickname
     * @return
     */
    @Transactional
    public Boolean requestFriend(String userNickname, String friendNickname) {
        User user = userRepository.findByNickname(userNickname).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND)
        );

        User friend = userRepository.findByNickname(friendNickname).orElseThrow(
                () -> new FriendException(ExceptionCodeSet.FRIEND_NOT_FOUND)
        );

        if (friendRepository.findFriendInProgress(user, friend).isPresent() || userNickname.equals(friendNickname)) {
            return false;
        }
        {
            Friend friendRelation = new Friend(user, friend, FriendStatus.Wait);
            friendRepository.save(friendRelation);
            return true;
        }
    }

    /*친구 요청 수락, 거절 등에 대한 처리*/
    @Transactional
    public FriendResponseDto.ResponseResult responseFriend(String userNickname, String friendNickname, FriendStatus status) {
        User user = userRepository.findByNickname(userNickname).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND)
        );

        User friend = userRepository.findByNickname(friendNickname).orElseThrow(
                () -> new FriendException(ExceptionCodeSet.FRIEND_NOT_FOUND)
        );

        Friend friendRelation = friendRepository.findRequestFriend(user, friend).orElseThrow(
                () -> new FriendException(ExceptionCodeSet.FRIEND_NOT_FOUND_REQ)
        );

        friendRelation.updateStatus(status);
        return new FriendResponseDto.ResponseResult(user.getNickname(), friend.getNickname(), friendRelation.getStatus());
    }

    /*친구 삭제*/
    @Transactional
    public Boolean deleteFriend(String userNickname, String friendNickname) {
        User user = userRepository.findByNickname(userNickname).orElseThrow(
                () -> new UserException(ExceptionCodeSet.USER_NOT_FOUND)
        );

        User friend = userRepository.findByNickname(friendNickname).orElseThrow(
                () -> new FriendException(ExceptionCodeSet.FRIEND_NOT_FOUND)
        );

        Optional<Friend> friendRelation = friendRepository.findFriendRelation(user, friend);

        if (friendRelation.isPresent()) {
            friendRepository.delete(friendRelation.get());
            return true;
        } else {
            return false;
        }
    }

    /*회원과 친구가 어떤 관계인지 나타내는 메소드*/
    public FriendStatus getFriendStatus(User user, User friend) {
        Optional<Friend> friendRelationOpt = friendRepository.findFriendRelation(user, friend);
        FriendStatus isFriend = null;

        if (friendRelationOpt.isPresent()) {
            Friend friendRelation = friendRelationOpt.get();

            if (friendRelation.getStatus() == FriendStatus.Accept) {
                isFriend = FriendStatus.Accept;
            } else if (friendRelation.getStatus() == FriendStatus.Wait) {
                if (friendRelation.getUser() == user)
                    isFriend = FriendStatus.Requesting;
                else
                    isFriend = FriendStatus.ResponseWait;
            }
        } else {
            isFriend = FriendStatus.NoFriend;
        }
        return isFriend;
    }
}
