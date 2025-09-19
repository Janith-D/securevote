// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract Voting {
    struct Candidate{
        uint id;
        string name;
        uint voteCount;
    }
    struct Voter {
        bool isRegistered;
        bool hasVoted;
        uint votedFor;
    }

    address public admin;
    mapping(address => Voter) public voters;
    Candidate[] public candidates;
    bool public votingOpen = false;

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }
    
    constructor(){
        admin = msg.sender;
    }
    function addCandidate(string memory _name) public onlyAdmin{
        candidates.push(Candidate({
            id: candidates.length,
            name: _name,
            voteCount: 0
        }));
    }
    function registerVoter(address _voter) public onlyAdmin {
        voters[_voter].isRegistered = true;
    }
    function startVoting() public onlyAdmin {
        votingOpen = true;
    }
    function endVoting() public onlyAdmin {
        votingOpen = false;
    }
    function vote(uint _candidateIndex) public{
        Voter storage sender = voters[msg.sender];
        require(votingOpen, "Voting is not open");
        require(sender.isRegistered, "You are not registered to vote");
        require(!sender.hasVoted, "You have already voted");
        require(_candidateIndex < candidates.length, "Invalid candidate index");


        sender.hasVoted = true;
        sender.votedFor = _candidateIndex;
        candidates[_candidateIndex].voteCount += 1;
    }
    function getCandidateCount() public view returns (uint) {
        return candidates.length;
    }
    function getVotesFor(uint _candidateIndex) public view returns (uint) {
        require(!votingOpen, "Voting is not open");
        return candidates[_candidateIndex].voteCount;
    }
}